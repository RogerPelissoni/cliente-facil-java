package br.com.clientefacil.messaging.template;

// Ver templates/email/password-reset.html — usado por AuthService.forgotPassword.
public record PasswordResetTemplate(String resetUrl) implements EmailTemplate {

    @Override
    public String templateName() {
        return "password-reset";
    }
}
