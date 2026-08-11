package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.EmailRabbitMQConfig;
import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.service.MailConfigService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
 * Circuit breaker (ver docs/guides/4_circuit-breaker-smtp.md): o envio de verdade
 * (emailSenderService.send) é protegido por um CircuitBreaker do Resilience4j, um por
 * empresa/config (circuitBreakerName) — se o SMTP de uma empresa está fora, cada mensagem dela não
 * precisa mais gastar o timeout de conexão inteiro pra descobrir de novo; o circuito abre depois de
 * falhas suficientes (ver application.yml, resilience4j.circuitbreaker.configs.default) e passa a
 * rejeitar na hora (CallNotPermittedException), sem tentar o SMTP. Isso não elimina o retry+DLQ da
 * mensagem em si (a exceção ainda propaga, então ainda vale as 3 tentativas de sempre) — só torna
 * cada uma dessas tentativas instantânea em vez de esperar um timeout de rede real, e evita que um
 * SMTP fora do ar de uma empresa trave a thread do listener martelando conexões que sabidamente vão
 * falhar. Circuito de uma empresa não afeta o de outra nem o da config base (nomes diferentes).
 * <p>
 * SIMULATE_FAILURE_VARIABLE: sentinela usado por NotificationDeadLetterService.simulateEmailFailure
 * pra testar o pipeline de retry+DLQ+alerta sob demanda (painel admin/dead-letters), sem depender de
 * um SMTP de verdade estar fora do ar. Checado antes de qualquer coisa, pra não gastar uma tentativa
 * real de envio nem contar pro circuit breaker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    public static final String SIMULATE_FAILURE_VARIABLE = "simulateFailure";

    private final MailConfigService mailConfigService;
    private final EmailSenderService emailSenderService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @RabbitListener(queues = EmailRabbitMQConfig.EMAIL_QUEUE)
    public void receive(EmailMessageDTO message) throws Exception {
        if (message.variables() != null && Boolean.TRUE.equals(message.variables().get(SIMULATE_FAILURE_VARIABLE))) {
            throw new RuntimeException("Falha simulada para testar o pipeline de DLQ (e-mail)");
        }

        MailConfig config = mailConfigService.resolveEffectiveConfig(message.companyId());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName(config));

        try {
            circuitBreaker.executeCallable(() -> {
                emailSenderService.send(config, message.to(), message.subject(), message.template(), message.variables());
                return null;
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuito de e-mail aberto para '{}' — envio de '{}' pra {} abortado sem tentar o SMTP " +
                            "(falhas recentes demais; volta a testar automaticamente em breve)",
                    circuitBreaker.getName(), message.subject(), message.to());
            throw e;
        }

        log.info("E-mail '{}' enviado para {} (template={}, companyId={})",
                message.subject(), message.to(), message.template(), message.companyId());
    }

    // Uma instância de circuito por config efetivamente usada (empresa ou base) — o SMTP de uma
    // empresa cair não deve abrir o circuito de outra nem o da config base do sistema. Config sem
    // id (ex: testDraft, que nunca passa por aqui) não é um cenário desta fila.
    private String circuitBreakerName(MailConfig config) {
        return "email-smtp-" + (config.getCompanyId() != null ? config.getCompanyId() : "base");
    }
}
