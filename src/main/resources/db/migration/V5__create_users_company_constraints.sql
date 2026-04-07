ALTER TABLE users
    ADD CONSTRAINT fk_users_person_id FOREIGN KEY (person_id) REFERENCES person (id),
    ADD CONSTRAINT fk_users_profile_id FOREIGN KEY (profile_id) REFERENCES profile (id),
    ADD CONSTRAINT fk_users_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE company
    ADD CONSTRAINT fk_company_person_id FOREIGN KEY (person_id) REFERENCES person (id),
    ADD CONSTRAINT fk_company_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_company_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE person
    ADD CONSTRAINT fk_person_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    ADD CONSTRAINT fk_person_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_person_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE profile
    ADD CONSTRAINT fk_profile_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    ADD CONSTRAINT fk_profile_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_profile_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
