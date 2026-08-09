package br.com.clientefacil.messaging.template;

// Ver templates/email/email-confirmation.html — usado por UserService.sendConfirmationEmail.
public record EmailConfirmationTemplate(String confirmUrl) implements EmailTemplate {

    @Override
    public String templateName() {
        return "email-confirmation";
    }
}
