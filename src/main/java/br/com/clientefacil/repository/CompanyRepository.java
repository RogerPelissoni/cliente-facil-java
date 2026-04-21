package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    @Query("""
                select c.id, c.name
                from Company c
                order by c.name
            """)
    List<Object[]> keyValue();

    Optional<Company> findByName(String name);

    @EntityGraph(attributePaths = {
            "person",
    })
    @Query("select c from Company c")
    Page<Company> findAllWithRelations(Pageable pageable);
}