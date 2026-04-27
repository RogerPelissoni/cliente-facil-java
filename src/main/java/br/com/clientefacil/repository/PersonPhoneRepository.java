package br.com.clientefacil.repository;

import br.com.clientefacil.entity.PersonPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonPhoneRepository extends JpaRepository<PersonPhone, Long>, JpaSpecificationExecutor<PersonPhone> {
}
