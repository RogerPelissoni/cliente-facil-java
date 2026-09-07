CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)    NOT NULL,
    -- Sem UNIQUE aqui: soft delete (deleted_at abaixo) precisa que a unicidade seja parcial, ver
    -- índice logo após o CREATE TABLE — senão um usuário excluído bloquearia pra sempre recriar
    -- outro com o mesmo e-mail.
    email      VARCHAR(255)    NOT NULL,
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
    updated_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- null = ativo (ver core/entity/AbstractSoftDeletableTenantEntity — repository.delete() vira um
    -- UPDATE preenchendo isto em vez de um DELETE físico, ver @SQLDelete em entity/User.java).
    deleted_at TIMESTAMP,

    -- Diferente de deleted_at acima: fl_active é uma flag de negócio (ativo/inativo), não exclusão.
    -- Existe porque, com o ForeignKeyDeletionGuard genérico, apagar um usuário que já criou/editou
    -- qualquer registro (created_by/updated_by, presente em quase toda tabela) fica bloqueado de
    -- propósito — a forma de "remover" um usuário do dia a dia é desativar, não excluir. Mesmo
    -- espírito de person.fl_active (V3_2__create_person_table.sql).
    fl_active  BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Índice único PARCIAL (só entre os ativos) em vez de UNIQUE de coluna — permite recriar um usuário
-- com o mesmo e-mail depois que o anterior foi soft-deletado. Mesmo padrão que
-- V14_2__create_mail_config_table.sql já usa.
CREATE UNIQUE INDEX users_email_key ON users (email) WHERE deleted_at IS NULL;
