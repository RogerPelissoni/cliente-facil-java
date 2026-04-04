-- users Constraints
ALTER TABLE users
    ADD CONSTRAINT fk_person_person_id FOREIGN KEY (person_id) REFERENCES person (id),
    ADD CONSTRAINT fk_profile_profile_id FOREIGN KEY (profile_id) REFERENCES profile(id),
    ADD CONSTRAINT fk_company_company_id FOREIGN KEY (company_id) REFERENCES company(id),
    ADD CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

-- company Constraints
ALTER TABLE company
    ADD CONSTRAINT fk_person_person_id FOREIGN KEY (person_id) REFERENCES person (id),
    ADD CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

-- person Constraints
ALTER TABLE person
    ADD CONSTRAINT fk_company_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    ADD CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

-- profile Constraints
ALTER TABLE profile
    ADD CONSTRAINT fk_company_company_id FOREIGN KEY (company_id) REFERENCES company (id),
    ADD CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);