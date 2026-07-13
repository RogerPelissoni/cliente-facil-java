package br.com.clientefacil.repository;

import br.com.clientefacil.core.dto.KeyValueDTO;
import br.com.clientefacil.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    @Query("""
                select
                    c.id as id,
                    p.name as name
                from Client c
                join c.person p
                order by p.name
            """)
    List<KeyValueDTO> keyValue();

    @EntityGraph(attributePaths = {"person"})
    @Query("select c from Client c")
    Page<Client> findAllWithRelations(Pageable pageable);
}