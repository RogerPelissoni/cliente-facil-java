CREATE TABLE module
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_module_name_company UNIQUE (name, company_id),

    CONSTRAINT fk_module_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_module_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_module_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
