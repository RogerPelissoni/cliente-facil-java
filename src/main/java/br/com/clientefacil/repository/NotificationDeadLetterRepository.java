package br.com.clientefacil.repository;

import br.com.clientefacil.entity.NotificationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, Long> {
}
