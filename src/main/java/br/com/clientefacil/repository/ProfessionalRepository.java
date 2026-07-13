package br.com.clientefacil.repository;

import br.com.clientefacil.core.dto.KeyValueDTO;
import br.com.clientefacil.entity.Professional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProfessionalRepository extends JpaRepository<Professional, Long>, JpaSpecificationExecutor<Professional> {
    @Query("""
                select
                    pro.id as id,
                    per.name as name
                from Professional pro
                join pro.person per
                order by per.name
            """)
    List<KeyValueDTO> keyValue();

    @EntityGraph(attributePaths = {"person"})
    @Query("select c from Professional c")
    Page<Professional> findAllWithRelations(Pageable pageable);
}