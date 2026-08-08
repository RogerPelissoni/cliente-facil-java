package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.MailEncryptionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Testa uma configuração de e-mail a partir dos dados ATUAIS do formulário, sem precisar salvar
// primeiro (diferente de MailConfigTestRequest, que só testa o que já está persistido). Mesma
// semântica de dsPassword em branco = usa a senha já salva para o scope informado (ver
// MailConfigService.testDraft) — assim testar depois de mudar só a porta, por exemplo, não obriga a
// redigitar a senha.
public record MailConfigDraftTestRequest(
        @NotNull MailConfigTestRequest.MailConfigScope scope,
        @NotBlank @Email String to,
        @NotBlank String dsHost,
        @NotNull @Min(1) @Max(65535) Integer nrPort,
        String dsUsername,
        String dsPassword,
        @NotNull MailEncryptionType tpEncryption,
        @NotBlank String dsFromName,
        @NotBlank @Email String dsFromAddress
) {
}
