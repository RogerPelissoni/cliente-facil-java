CREATE TABLE event
(
    id             BIGSERIAL PRIMARY KEY,

    ds_title       VARCHAR(100)      NOT NULL,
    ds_description TEXT,
    dt_start       TIMESTAMP         NOT NULL,
    dt_end         TIMESTAMP         NOT NULL,
    tp_status      event_status_enum NOT NULL,
    tp_event       event_type_enum   NOT NULL,

    company_id     BIGINT,
    created_by     BIGINT,
    updated_by     BIGINT,
    created_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_event_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);