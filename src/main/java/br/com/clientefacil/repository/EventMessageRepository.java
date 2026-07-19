package br.com.clientefacil.repository;

import br.com.clientefacil.entity.EventMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EventMessageRepository extends JpaRepository<EventMessage, Long>, JpaSpecificationExecutor<EventMessage> {
    List<EventMessage> findEntityByEventId(Long idEvent);
}
