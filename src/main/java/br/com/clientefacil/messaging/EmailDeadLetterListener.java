package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.EmailRabbitMQConfig;
import br.com.clientefacil.entity.NotificationDeadLetter;
import br.com.clientefacil.entity.enums.DeadLetterOriginEnum;
import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Consome a DLQ de e-mail (ver EmailRabbitMQConfig): mensagens que esgotaram as tentativas de
 * envio do EmailListener caem aqui. Reaproveita a mesma tabela/entidade de
 * NotificationDeadLetterListener (notification_dead_letter, tpOrigin=EMAIL) — auditoria idêntica,
 * mesmo tratamento defensivo do header x-death.
 * <p>
 * Deliberadamente NÃO publica outro e-mail (nem nesta fila, nem em nenhuma outra) para avisar
 * sobre a própria falha: se a causa for sistêmica (ex: SMTP fora do ar), isso geraria um novo
 * e-mail que também falharia, em loop. O aviso fica restrito ao broadcast em tempo real — o mesmo
 * canal que a Parte 7 de mensageria já usa para dead letters de notificação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDeadLetterListener {

    private final NotificationDeadLetterRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = EmailRabbitMQConfig.EMAIL_DLQ)
    public void receive(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        DeathInfo deathInfo = extractDeathInfo(message);

        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setTpOrigin(DeadLetterOriginEnum.EMAIL);
        entity.setDsPayload(payload);
        entity.setDsErrorReason(deathInfo.reason());
        entity.setNrDeathCount(deathInfo.count());
        entity.setDtFailedAt(deathInfo.failedAt());
        entity = repository.save(entity);

        log.error("Mensagem movida para a DLQ de e-mail (id={}, motivo={}, tentativas={}): {}",
                entity.getId(), entity.getDsErrorReason(), entity.getNrDeathCount(), payload);

        messagingTemplate.convertAndSend(NotificationDeadLetterListener.DEAD_LETTER_DESTINATION,
                new NotificationDeadLetterAlert(entity.getId(), entity.getDsErrorReason(),
                        entity.getNrDeathCount(), entity.getDtFailedAt()));
    }

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
