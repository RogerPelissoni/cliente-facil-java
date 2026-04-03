CREATE TABLE "users"
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,

    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,

    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT fk_user_created_by
        FOREIGN KEY (created_by) REFERENCES "users" (id),

    CONSTRAINT fk_user_updated_by
        FOREIGN KEY (updated_by) REFERENCES "users" (id)
);