CREATE TABLE client
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    email      VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);