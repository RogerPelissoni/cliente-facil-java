CREATE TABLE person_adress
(
    id            BIGSERIAL PRIMARY KEY,

    ds_street     VARCHAR(255),
    ds_number     VARCHAR(50),
    ds_complement VARCHAR(255),
    ds_district   VARCHAR(255),
    ds_city       VARCHAR(255) NOT NULL,
    ds_state      VARCHAR(255) NOT NULL,
    ds_zip_code   VARCHAR(255) NOT NULL,
    fl_main       BOOLEAN,

    person_id     BIGINT,

    company_id    BIGINT,
    created_by    BIGINT,
    updated_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_person_adress_person_id FOREIGN KEY (person_id) REFERENCES person (id),

    CONSTRAINT fk_person_adress_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_person_adress_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_person_adress_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
