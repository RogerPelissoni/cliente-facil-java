package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    @Query("""
                select c.id, c.name
                from Company c
                order by c.name
            """)
    List<Object[]> keyValue();
    
    Optional<Company> findByName(String name);
}