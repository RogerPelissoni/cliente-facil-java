package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Resource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findBySignature(String signature);

    boolean existsByModuleId(Long moduleId);

    @EntityGraph(attributePaths = "module")
    @Query("select r from Resource r")
    List<Resource> findAllWithRelations();
}