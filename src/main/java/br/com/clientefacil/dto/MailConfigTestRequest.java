package br.com.clientefacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MailConfigTestRequest(
        @NotNull MailConfigScope scope,
        @NotBlank @Email String to
) {
    public enum MailConfigScope {
        BASE,    // usa a config base do sistema (company_id IS NULL)
        COMPANY  // usa a config da empresa autenticada (cai pra base se não existir)
    }
}
