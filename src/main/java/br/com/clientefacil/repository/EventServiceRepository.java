package br.com.clientefacil.repository;

import br.com.clientefacil.entity.EventService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventServiceRepository extends JpaRepository<EventService, Long>, JpaSpecificationExecutor<EventService> {
}
