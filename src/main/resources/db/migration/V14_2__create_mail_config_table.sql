-- mail_config: configuração de SMTP usada pelo EmailListener para montar um JavaMailSender
-- dinâmico. company_id NULL = "config base" do sistema (usada por e-mails que não pertencem
-- a uma empresa específica, ex: alerta de dead-letter, recuperação de senha). company_id
-- preenchido = config própria de uma empresa, que tem prioridade sobre a base quando existir.
-- Segue o mesmo mecanismo de fallback já usado pelo tenantFilter do Hibernate
-- (company_id = :companyId OR company_id IS NULL).
CREATE TABLE mail_config
(
    id             BIGSERIAL PRIMARY KEY,

    company_id     BIGINT,

    ds_host        VARCHAR(255)               NOT NULL,
    nr_port        INTEGER                    NOT NULL,
    ds_username    VARCHAR(255),
    ds_password    TEXT,
    tp_encryption  mail_encryption_type_enum  NOT NULL DEFAULT 'TLS',

    ds_from_name   VARCHAR(255)               NOT NULL,
    ds_from_address VARCHAR(255)               NOT NULL,

    fl_active      BOOLEAN                    NOT NULL DEFAULT TRUE,

    created_by     BIGINT,
    updated_by     BIGINT,
    created_at     TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mail_config_company_id FOREIGN KEY (company_id) REFERENCES company (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_mail_config_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mail_config_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- No máximo 1 config base (company_id IS NULL) e no máximo 1 config por empresa.
CREATE UNIQUE INDEX idx_mail_config_single_base ON mail_config ((TRUE)) WHERE company_id IS NULL;
CREATE UNIQUE INDEX idx_mail_config_company_unique ON mail_config (company_id) WHERE company_id IS NOT NULL;
