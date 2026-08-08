-- Token de uso único pra confirmação de e-mail e recuperação de senha (ver UserTokenService).
-- Não é tenant-scoped: infraestrutura de autenticação, não dado de negócio de uma empresa —
-- mesmo espírito de notification_dead_letter.
CREATE TABLE user_token
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    tp_type        VARCHAR(30)  NOT NULL,
    ds_token_hash  VARCHAR(255) NOT NULL UNIQUE,
    dt_expires_at  TIMESTAMP    NOT NULL,
    dt_used_at     TIMESTAMP,

    created_by     BIGINT,
    updated_by     BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_user_token_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_user_token_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_user_token_type CHECK (tp_type IN ('EMAIL_CONFIRMATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_user_token_user ON user_token (user_id);
