package br.com.clientefacil.messaging;

import java.util.List;
import java.util.Map;

/**
 * Corpo que trafega pela fila de e-mail (EmailPublisher escreve, EmailListener lê e envia).
 * <p>
 * companyId: null = usa a config base do sistema (ver MailConfigService); preenchido = usa a
 * config da empresa, se existir (senão cai pra base).
 * template: nome do arquivo em resources/templates/email (sem extensão), ex: "dead-letter-alert".
 * variables: dados injetados no template Thymeleaf — só tipos simples (String, número, data já
 * formatada), já que o payload trafega como JSON pelo RabbitMQ.
 */
public record EmailMessageDTO(
        Long companyId,
        List<String> to,
        String subject,
        String template,
        Map<String, Object> variables
) {
}
