package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.RabbitMQConfig;
import br.com.clientefacil.domain.config.ResourceEnum;
import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.entity.NotificationDeadLetter;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import br.com.clientefacil.repository.UserRepository;
import br.com.clientefacil.service.EmailService;
import br.com.clientefacil.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Consome a DLQ (ver RabbitMQConfig): mensagens que já esgotaram as tentativas de reprocessamento
 * do NotificationListener caem aqui. Este listener não tenta reprocessar nada — só registra o que
 * aconteceu (auditoria mínima em banco, já que a DLQ do RabbitMQ sozinha não é consultável junto
 * com o resto dos dados da aplicação nem sobrevive a um reset do ambiente) e avisa em tempo real
 * quem estiver conectado ao canal de alertas.
 * <p>
 * Consome como Message (envelope cru do AMQP), não como NotificationMessageDTO: a mensagem já
 * demonstrou que pode estar malformada — é por causa disso que ela chegou até aqui — então
 * evitamos repetir a mesma conversão para DTO que originalmente falhou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeadLetterListener {

    // Broadcast, não por usuário: infraestrutura pronta para um futuro painel operacional
    // acompanhar falhas de mensageria em tempo real — mesma ideia de reuso genérico do StompProvider,
    // só que num destino "/topic" (n:n) em vez de "/queue" (1:1, ver NotificationListener).
    public static final String DEAD_LETTER_DESTINATION = "/topic/system/dead-letters";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final NotificationDeadLetterRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_DLQ)
    public void receive(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        DeathInfo deathInfo = extractDeathInfo(message);

        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDsPayload(payload);
        entity.setDsErrorReason(deathInfo.reason());
        entity.setNrDeathCount(deathInfo.count());
        entity.setDtFailedAt(deathInfo.failedAt());
        entity = repository.save(entity);

        log.error("Mensagem movida para a DLQ de notificações (id={}, motivo={}, tentativas={}): {}",
                entity.getId(), entity.getDsErrorReason(), entity.getNrDeathCount(), payload);

        messagingTemplate.convertAndSend(DEAD_LETTER_DESTINATION, new NotificationDeadLetterAlert(
                entity.getId(), entity.getDsErrorReason(), entity.getNrDeathCount(), entity.getDtFailedAt()));

        notifyAdmins(entity);
    }

    // Gera uma notificação real (sino) para quem administra a mensageria — não passa pela fila de
    // novo (ver javadoc da classe): persiste e empurra via STOMP diretamente, do mesmo jeito que
    // NotificationListener faz para o fluxo normal, só que sem depender do RabbitMQ para isso.
    // Best-effort: uma falha aqui (ex: nenhum admin cadastrado) não pode derrubar o processamento
    // da DLQ em si, então erros só são logados.
    //
    // Também dispara um e-mail (via EmailService -> fila própria de e-mail, companyId=null =
    // config base do sistema) para quem tem DEAD_LETTER_VIEW e tem e-mail cadastrado — cobre o
    // caso de ninguém estar com o painel aberto no momento da falha (ver "Limitações conhecidas"
    // em docs/guides/1_messaging-and-websocket.md). Enfileirar o e-mail é best-effort igual ao resto deste
    // método: uma falha ao publicar não pode derrubar o processamento da DLQ.
    private void notifyAdmins(NotificationDeadLetter deadLetter) {
        try {
            List<Object[]> admins = userRepository.findIdAndEmailByResourceSignature(
                    ResourceEnum.DEAD_LETTER_VIEW.getSignature());

            String message = "Uma mensagem de notificação (motivo: %s) esgotou as tentativas de reprocessamento e caiu na fila de falhas (registro #%d)."
                    .formatted(deadLetter.getDsErrorReason(), deadLetter.getId());

            for (Object[] admin : admins) {
                Long adminUserId = (Long) admin[0];
                String adminEmail = (String) admin[1];

                NotificationMessageDTO alert = new NotificationMessageDTO(
                        adminUserId, NotificationTypeEnum.ERROR, "Falha no processamento de notificação", message);

                NotificationResponse notification = notificationService.create(alert);

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(adminUserId), NotificationListener.NOTIFICATION_DESTINATION, notification);

                sendEmailAlert(deadLetter, adminEmail);
            }
        } catch (Exception e) {
            log.warn("Não foi possível gerar a notificação de alerta para os administradores", e);
        }
    }

    private void sendEmailAlert(NotificationDeadLetter deadLetter, String adminEmail) {
        if (!StringUtils.hasText(adminEmail)) {
            return;
        }

        try {
            emailService.sendTemplated(null, adminEmail, "Cliente Fácil — Falha no processamento de mensageria",
                    "dead-letter-alert", Map.of(
                            "origin", deadLetter.getTpOrigin().name(),
                            "deadLetterId", deadLetter.getId(),
                            "reason", deadLetter.getDsErrorReason() != null ? deadLetter.getDsErrorReason() : "não informado",
                            "deathCount", deadLetter.getNrDeathCount() != null ? deadLetter.getNrDeathCount() : "-",
                            "failedAt", deadLetter.getDtFailedAt() != null ? deadLetter.getDtFailedAt().format(DATE_FORMAT) : "-"
                    ));
        } catch (Exception e) {
            log.warn("Não foi possível enfileirar o e-mail de alerta para {}", adminEmail, e);
        }
    }

    // "x-death" é o header que o próprio RabbitMQ adiciona ao redirecionar uma mensagem para uma
    // DLQ (motivo, quantidade de tentativas, quando aconteceu). Parsing defensivo porque é uma
    // lista de mapas heterogênea cujo formato exato não é garantido pelo broker.
    @SuppressWarnings("unchecked")
    private DeathInfo extractDeathInfo(Message message) {
        try {
            Object header = message.getMessageProperties().getHeaders().get("x-death");

            if (header instanceof List<?> deaths && !deaths.isEmpty() && deaths.get(0) instanceof Map<?, ?> first) {
                Map<String, Object> death = (Map<String, Object>) first;

                String reason = death.get("reason") != null ? String.valueOf(death.get("reason")) : null;
                Integer count = death.get("count") instanceof Number number ? number.intValue() : null;
                LocalDateTime failedAt = death.get("time") instanceof Date time
                        ? LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault())
                        : LocalDateTime.now();

                return new DeathInfo(reason, count, failedAt);
            }
        } catch (Exception e) {
            log.warn("Não foi possível interpretar o header x-death da mensagem morta", e);
        }

        return new DeathInfo(null, null, LocalDateTime.now());
    }

    private record DeathInfo(String reason, Integer count, LocalDateTime failedAt) {
    }
}
