package br.com.clientefacil.entity.enums;

public enum MailEncryptionType {
    NONE, // Sem criptografia (dev/local, ex: MailHog)
    SSL,  // SMTPS (conexão já criptografada, ex: porta 465)
    TLS   // STARTTLS (upgrade da conexão para TLS, ex: porta 587)
}
