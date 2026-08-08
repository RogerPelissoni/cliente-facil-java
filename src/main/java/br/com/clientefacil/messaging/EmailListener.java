package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.EmailRabbitMQConfig;
import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.service.MailConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer da fila de e-mail: resolve a config SMTP efetiva (empresa ou base — ver
 * MailConfigService) e delega a montagem/envio pro EmailSenderService (reaproveitado também pelo
 * teste síncrono de configuração em rascunho, ver MailConfigService.testDraft).
 * <p>
 * Qualquer exceção aqui (SMTP fora do ar, config inexistente, falha de autenticação) propaga e
 * aciona o retry+DLQ padrão do Spring AMQP (mesmo mecanismo do NotificationListener) — depois de 3
 * tentativas, a mensagem cai na fila clientefacil.email.queue.dlq (ver EmailDeadLetterListener).
 * <p>
 * SIMULATE_FAILURE_VARIABLE: sentinela usado por NotificationDeadLetterService.simulateEmailFailure
 * pra testar o pipeline de retry+DLQ+alerta sob demanda (painel admin/dead-letters), sem depender de
 * um SMTP de verdade estar fora do ar. Checado antes de qualquer coisa, pra não gastar uma tentativa
 * real de envio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    public static final String SIMULATE_FAILURE_VARIABLE = "simulateFailure";

    private final MailConfigService mailConfigService;
    private final EmailSenderService emailSenderService;

    @RabbitListener(queues = EmailRabbitMQConfig.EMAIL_QUEUE)
    public void receive(EmailMessageDTO message) throws Exception {
        if (message.variables() != null && Boolean.TRUE.equals(message.variables().get(SIMULATE_FAILURE_VARIABLE))) {
            throw new RuntimeException("Falha simulada para testar o pipeline de DLQ (e-mail)");
        }

        MailConfig config = mailConfigService.resolveEffectiveConfig(message.companyId());

        emailSenderService.send(config, message.to(), message.subject(), message.template(), message.variables());

        log.info("E-mail '{}' enviado para {} (template={}, companyId={})",
                message.subject(), message.to(), message.template(), message.companyId());
    }
}
