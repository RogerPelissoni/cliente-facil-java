package br.com.clientefacil.repository;

import br.com.clientefacil.entity.AccountReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, Long>, JpaSpecificationExecutor<AccountReceivable> {
    @Query("""
                select a.dsCode
                from AccountReceivable a
                order by a.dsCode desc
                limit 1
            """)
    Optional<String> findLastCode();
}
