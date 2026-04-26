package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {
    @Query("""
                    select p.id, p.name
                    from Person p
                    order by p.name
            """)
    List<Object[]> keyValue();

    Optional<Person> findByDsDocument(String document);
}
