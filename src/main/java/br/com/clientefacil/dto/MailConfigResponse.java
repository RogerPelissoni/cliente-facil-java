package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.MailEncryptionType;

// Nunca inclui a senha (nem criptografada) — só o necessário pra tela confirmar o que está
// configurado.
public record MailConfigResponse(
        Long id,
        Long companyId,
        String dsHost,
        Integer nrPort,
        String dsUsername,
        boolean hasPassword,
        MailEncryptionType tpEncryption,
        String dsFromName,
        String dsFromAddress,
        boolean flActive
) {
}
