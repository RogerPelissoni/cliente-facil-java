CREATE TABLE profile_permission
(
    id          BIGSERIAL PRIMARY KEY,

    profile_id  BIGINT,
    resource_id BIGINT,

    company_id  BIGINT,
    created_by  BIGINT,
    updated_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_profile_permission_profile_resource_company UNIQUE (profile_id, resource_id, company_id),

    CONSTRAINT fk_profile_permission_profile_id FOREIGN KEY (profile_id) REFERENCES profile (id),
    CONSTRAINT fk_profile_permission_resource_id FOREIGN KEY (resource_id) REFERENCES resource (id),
    CONSTRAINT fk_profile_permission_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_profile_permission_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_profile_permission_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);