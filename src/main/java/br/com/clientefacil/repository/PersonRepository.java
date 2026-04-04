package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByDsDocument(String document);
}
