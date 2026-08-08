CREATE TYPE mail_encryption_type_enum AS ENUM (
    'NONE', -- Sem criptografia (não recomendado, só para SMTP local/dev, ex: MailHog)
    'SSL',  -- SMTPS (conexão já criptografada, ex: porta 465)
    'TLS'   -- STARTTLS (upgrade da conexão para TLS, ex: porta 587)
);
