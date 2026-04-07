CREATE TABLE person
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    ds_document VARCHAR(20),
    tp_gender   person_gender_enum,
    fl_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    
    company_id  BIGINT,
    created_by  BIGINT,
    updated_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
