package br.com.clientefacil.controller;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.dto.NotificationSendRequest;
import br.com.clientefacil.dto.NotificationTestRequest;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import br.com.clientefacil.messaging.NotificationMessageDTO;
import br.com.clientefacil.messaging.NotificationPublisher;
import br.com.clientefacil.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final NotificationPublisher publisher;

    @Operation(summary = "FIND_MINE")
    @GetMapping
    public List<NotificationResponse> findMine() {
        return service.findMine();
    }

    @Operation(summary = "MARK_ALL_AS_READ")
    @PatchMapping("/read-all")
    public List<NotificationResponse> markAllAsRead() {
        return service.markAllAsRead();
    }

    @Operation(summary = "MARK_AS_READ")
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "PUBLISH_TEST_NOTIFICATION")
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void publishTest(@RequestBody @Valid NotificationTestRequest request) {
        NotificationTypeEnum type = request.type() != null ? request.type() : NotificationTypeEnum.INFO;

        publisher.publish(new NotificationMessageDTO(currentUserId(), type, "Notificação de teste", request.content()));
    }

    @Operation(summary = "SEND_TO_USERS")
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void send(@RequestBody @Valid NotificationSendRequest request) {
        NotificationTypeEnum type = request.type() != null ? request.type() : NotificationTypeEnum.INFO;

        request.userIds().forEach(userId ->
                publisher.publish(new NotificationMessageDTO(userId, type, request.title(), request.message())));
    }

    private Long currentUserId() {
        return SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
