package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

}
