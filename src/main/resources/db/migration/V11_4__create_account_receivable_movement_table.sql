CREATE TABLE account_receivable_movement
(
    id                    BIGSERIAL PRIMARY KEY,

    account_receivable_id BIGINT                                        NOT NULL,

    vl_movement           DECIMAL                                       NOT NULL,
    vl_discount           DECIMAL                                       NOT NULL,
    dt_movement           TIMESTAMP                                     NOT NULL,
    tp_payment            account_receivable_movement_payment_type_enum NOT NULL,
    ds_observations       TEXT,

    company_id            BIGINT,
    created_by            BIGINT,
    updated_by            BIGINT,
    created_at            TIMESTAMP                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_receivable_movement_account_receivable_id FOREIGN KEY (account_receivable_id) REFERENCES account_receivable (id),
    CONSTRAINT fk_account_receivable_movement_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_account_receivable_movement_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_account_receivable_movement_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);