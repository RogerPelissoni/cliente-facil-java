CREATE TABLE person_phone
(
    id         BIGSERIAL PRIMARY KEY,

    ds_phone   VARCHAR(255),
    fl_main    BOOLEAN,

    person_id  BIGINT,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_person_phone_person_id FOREIGN KEY (person_id) REFERENCES person (id),

    CONSTRAINT fk_person_phone_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_person_phone_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_person_phone_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
