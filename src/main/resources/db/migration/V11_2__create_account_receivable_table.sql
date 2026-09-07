CREATE TABLE account_receivable
(
    id             BIGSERIAL PRIMARY KEY,

    person_id      BIGINT                         NOT NULL,

    ds_code        VARCHAR(50)                    NOT NULL,
    nr_installment INT                            NOT NULL,

    vl_total       DECIMAL                        NOT NULL,
    vl_balance     DECIMAL                        NOT NULL,

    da_due         DATE                           NOT NULL,
    dt_paid        TIMESTAMP,

    tp_status      account_receivable_status_enum NOT NULL,
    ds_observation TEXT,

    company_id     BIGINT,
    created_by     BIGINT,
    updated_by     BIGINT,
    created_at     TIMESTAMP                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- null = ativo (ver core/entity/AbstractSoftDeletableTenantEntity).
    deleted_at     TIMESTAMP,

    CONSTRAINT fk_account_receivable_person_id FOREIGN KEY (person_id) REFERENCES person (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_account_receivable_company_id FOREIGN KEY (company_id) REFERENCES company (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_account_receivable_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_account_receivable_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Índice único PARCIAL em vez de constraint de coluna — mesmo motivo de uk_client_person_company.
CREATE UNIQUE INDEX uk_account_receivable_title_installment_company ON account_receivable (ds_code, nr_installment, company_id) WHERE deleted_at IS NULL;