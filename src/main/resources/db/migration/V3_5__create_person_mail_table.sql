CREATE TABLE person_mail
(
    id         BIGSERIAL PRIMARY KEY,

    ds_mail    VARCHAR(255),
    fl_main    BOOLEAN,

    person_id  BIGINT,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_person_mail_person_id FOREIGN KEY (person_id) REFERENCES person (id),

    CONSTRAINT fk_person_mail_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_person_mail_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_person_mail_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
