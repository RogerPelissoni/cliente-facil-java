CREATE TABLE resource
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    signature  VARCHAR(100) NOT NULL,

    module_id  BIGINT,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_resource_signature_company UNIQUE (signature, company_id),

    CONSTRAINT fk_resource_module_id FOREIGN KEY (module_id) REFERENCES module (id),
    CONSTRAINT fk_resource_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_resource_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_resource_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
