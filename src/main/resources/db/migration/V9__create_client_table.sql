CREATE TABLE client
(
    id         BIGSERIAL PRIMARY KEY,

    person_id  BIGINT,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- null = ativo (ver core/entity/AbstractSoftDeletableTenantEntity).
    deleted_at TIMESTAMP,

    CONSTRAINT fk_client_person_id FOREIGN KEY (person_id) REFERENCES person (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_client_company_id FOREIGN KEY (company_id) REFERENCES company (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_client_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_client_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Índice único PARCIAL em vez de constraint de coluna: permite recriar um Client pra
-- pessoa+empresa depois que o anterior foi soft-deletado (ver deleted_at acima).
CREATE UNIQUE INDEX uk_client_person_company ON client (person_id, company_id) WHERE deleted_at IS NULL;