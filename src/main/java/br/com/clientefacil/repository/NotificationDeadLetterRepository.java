package br.com.clientefacil.repository;

import br.com.clientefacil.entity.NotificationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface NotificationDeadLetterRepository
        extends JpaRepository<NotificationDeadLetter, Long>, JpaSpecificationExecutor<NotificationDeadLetter> {

    long countByDtResolvedIsNull();

    long countByDtResolvedIsNotNull();

    long countByCreatedAtAfter(LocalDateTime threshold);
}
