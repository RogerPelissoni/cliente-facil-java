CREATE TABLE event_service
(
    id                    BIGSERIAL PRIMARY KEY,

    event_id              BIGINT    NOT NULL,
    client_id             BIGINT    NOT NULL,
    professional_id       BIGINT    NOT NULL,
    account_receivable_id BIGINT    NOT NULL,

    company_id            BIGINT,
    created_by            BIGINT,
    updated_by            BIGINT,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_service_event_id FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_event_service_client_id FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_event_service_professional_id FOREIGN KEY (professional_id) REFERENCES professional (id),
    CONSTRAINT fk_event_service_account_receivable_id FOREIGN KEY (account_receivable_id) REFERENCES account_receivable (id),
    CONSTRAINT fk_event_service_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_event_service_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_event_service_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);