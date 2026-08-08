CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)    NOT NULL,
    email      VARCHAR(255)    NOT NULL UNIQUE,
    password   VARCHAR(255)    NOT NULL,
    role       users_role_enum NOT NULL,

    person_id  BIGINT          NOT NULL,
    profile_id BIGINT          NOT NULL,

    -- null = e-mail ainda não confirmado (login bloqueado, ver AuthService.login). Preenchido só
    -- via POST /auth/confirm-email (token enviado no momento da criação do usuário).
    dt_email_confirmed_at TIMESTAMP,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
