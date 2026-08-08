package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.MailEncryptionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// dsPassword é opcional: se vier em branco/nula numa atualização, a senha já salva é mantida (ver
// MailConfigService) — evita forçar o front a reenviar a senha em toda edição, já que ela nunca é
// devolvida pela API (MailConfig.dsPassword é WRITE_ONLY).
public record MailConfigRequest(
        @NotBlank String dsHost,
        @NotNull @Min(1) @Max(65535) Integer nrPort,
        String dsUsername,
        String dsPassword,
        @NotNull MailEncryptionType tpEncryption,
        @NotBlank String dsFromName,
        @NotBlank @Email String dsFromAddress,
        boolean flActive
) {
}
