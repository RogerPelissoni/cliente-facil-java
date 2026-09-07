CREATE TABLE company
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,

    person_id  BIGINT,
    
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- null = ativo (ver core/entity/AbstractSoftDeletableEntity). O ForeignKeyDeletionGuard genérico
    -- bloqueia exclusão de Company com qualquer dado ativo do tenant (praticamente sempre, já que
    -- quase toda tabela tem company_id) — arquivar um tenant na prática é desativar (fl_active
    -- abaixo), não excluir (ver docs/product/2_known-limitations.md).
    deleted_at TIMESTAMP,

    -- Ver comentário equivalente em V1_2__create_users_table.sql — mesmo papel, pro tenant inteiro.
    fl_active  BOOLEAN      NOT NULL DEFAULT TRUE
);
