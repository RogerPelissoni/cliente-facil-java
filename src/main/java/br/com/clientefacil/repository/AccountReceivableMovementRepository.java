package br.com.clientefacil.repository;

import br.com.clientefacil.entity.AccountReceivableMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountReceivableMovementRepository extends JpaRepository<AccountReceivableMovement, Long>, JpaSpecificationExecutor<AccountReceivableMovement> {

    boolean existsByAccountReceivableId(Long accountReceivableId);
}
