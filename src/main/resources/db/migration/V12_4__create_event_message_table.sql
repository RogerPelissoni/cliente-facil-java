CREATE TABLE event_message
(
    id         BIGSERIAL PRIMARY KEY,

    event_id   BIGINT    NOT NULL,
    ds_message TEXT      NOT NULL,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_message_event_id FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_event_message_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_event_message_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_event_message_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);