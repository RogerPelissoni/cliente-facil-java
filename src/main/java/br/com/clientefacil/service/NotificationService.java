package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.entity.Notification;
import br.com.clientefacil.entity.enums.NotificationStatusEnum;
import br.com.clientefacil.mapper.NotificationMapper;
import br.com.clientefacil.messaging.NotificationMessageDTO;
import br.com.clientefacil.repository.NotificationRepository;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;

    public NotificationResponse create(NotificationMessageDTO message) {
        Notification entity = new Notification();
        entity.setUser(userRepository.getReferenceById(message.userId()));
        entity.setTpType(message.type());
        entity.setTpStatus(NotificationStatusEnum.UNREAD);
        entity.setDsTitle(message.title());
        entity.setDsMessage(message.message());

        return mapper.toResponse(repository.save(entity));
    }

    public List<NotificationResponse> findMine() {
        return mapper.toResponseList(repository.findTop50ByUserIdOrderByCreatedAtDesc(currentUserId()));
    }

    public List<NotificationResponse> markAllAsRead() {
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<Notification> unread = repository.findByUserIdAndTpStatus(userId, NotificationStatusEnum.UNREAD);

        unread.forEach(notification -> {
            notification.setTpStatus(NotificationStatusEnum.READ);
            notification.setDtRead(now);
        });

        repository.saveAll(unread);

        return findMine();
    }

    public NotificationResponse markAsRead(Long id) {
        Notification entity = findOwnedEntity(id);

        if (entity.getTpStatus() == NotificationStatusEnum.UNREAD) {
            entity.setTpStatus(NotificationStatusEnum.READ);
            entity.setDtRead(LocalDateTime.now());
            repository.save(entity);
        }

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        repository.delete(findOwnedEntity(id));
    }

    private Notification findOwnedEntity(Long id) {
        Notification entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));

        if (!entity.getUser().getId().equals(currentUserId())) {
            throw new AccessDeniedException("Notificação não pertence ao usuário autenticado");
        }

        return entity;
    }

    private Long currentUserId() {
        return SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
