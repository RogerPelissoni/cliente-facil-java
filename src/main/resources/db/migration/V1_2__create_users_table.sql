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

    -- Bloqueio de conta após tentativas de login com senha errada (ver AuthService.login).
    -- nr_failed_login_attempts zera a cada login bem-sucedido; ao atingir o limite,
    -- dt_locked_until é preenchido e o contador zera de novo (a próxima janela de tentativas
    -- começa do zero quando o bloqueio expirar).
    nr_failed_login_attempts INT NOT NULL DEFAULT 0,
    dt_locked_until          TIMESTAMP,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
