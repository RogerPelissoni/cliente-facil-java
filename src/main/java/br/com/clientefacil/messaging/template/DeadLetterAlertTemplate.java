package br.com.clientefacil.messaging.template;

// Ver templates/email/dead-letter-alert.html — usado por NotificationDeadLetterListener.sendEmailAlert.
// reason/deathCount podem ser null: o próprio template já trata isso (`${reason} ?: 'não informado'`,
// `${deathCount} ?: '-'`) — não precisa duplicar esse fallback aqui. failedAt não tem fallback no
// template, então precisa vir já formatado (nunca null) pelo chamador.
public record DeadLetterAlertTemplate(
        String origin,
        Long deadLetterId,
        String reason,
        Integer deathCount,
        String failedAt
) implements EmailTemplate {

    @Override
    public String templateName() {
        return "dead-letter-alert";
    }
}
