package br.com.clientefacil.messaging.template;

// Ver templates/email/test-email.html — usado por EmailService.sendTest e MailConfigService.testDraft.
public record TestEmailTemplate(String sentAt) implements EmailTemplate {

    @Override
    public String templateName() {
        return "test-email";
    }
}
