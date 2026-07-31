package br.com.clientefacil.repository;

import br.com.clientefacil.entity.Notification;
import br.com.clientefacil.entity.enums.NotificationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTpStatus(Long userId, NotificationStatusEnum tpStatus);
}
