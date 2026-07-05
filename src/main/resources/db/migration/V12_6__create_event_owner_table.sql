CREATE TABLE event_owner
(
    id         BIGSERIAL PRIMARY KEY,

    event_id   BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_event_owner_event_users UNIQUE (event_id, user_id),

    CONSTRAINT fk_event_owner_event_id FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_event_owner_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_event_owner_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_event_owner_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_event_owner_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);