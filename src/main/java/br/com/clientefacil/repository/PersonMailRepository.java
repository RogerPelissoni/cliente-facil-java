package br.com.clientefacil.repository;

import br.com.clientefacil.entity.PersonMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonMailRepository extends JpaRepository<PersonMail, Long>, JpaSpecificationExecutor<PersonMail> {
}
