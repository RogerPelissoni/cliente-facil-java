CREATE TABLE notification_dead_letter
(
    id              BIGSERIAL PRIMARY KEY,

    ds_payload      TEXT      NOT NULL,
    ds_error_reason VARCHAR(255),
    nr_death_count  INTEGER,
    dt_failed_at    TIMESTAMP,

    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_dead_letter_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_notification_dead_letter_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT
);
