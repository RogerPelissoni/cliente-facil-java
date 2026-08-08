CREATE TABLE notification_dead_letter
(
    id              BIGSERIAL PRIMARY KEY,

    -- Origem da mensagem morta: qual fila/pipeline a gerou. Reaproveitamos esta mesma
    -- tabela para a DLQ de e-mail (ver EmailDeadLetterListener) em vez de criar uma tabela
    -- dedicada, já que a auditoria é idêntica (log operacional de mensageria).
    tp_origin       VARCHAR(20) NOT NULL DEFAULT 'NOTIFICATION',

    ds_payload      TEXT      NOT NULL,
    ds_error_reason VARCHAR(255),
    nr_death_count  INTEGER,
    dt_failed_at    TIMESTAMP,
    dt_resolved     TIMESTAMP,

    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_dead_letter_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_notification_dead_letter_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_notification_dead_letter_origin CHECK (tp_origin IN ('NOTIFICATION', 'EMAIL'))
);

CREATE INDEX idx_notification_dead_letter_origin ON notification_dead_letter (tp_origin);
