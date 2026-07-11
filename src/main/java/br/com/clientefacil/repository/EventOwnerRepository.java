package br.com.clientefacil.repository;

import br.com.clientefacil.entity.EventOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventOwnerRepository extends JpaRepository<EventOwner, Long>, JpaSpecificationExecutor<EventOwner> {
}
