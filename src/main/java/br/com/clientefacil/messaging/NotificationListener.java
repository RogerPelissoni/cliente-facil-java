package br.com.clientefacil.messaging;

import br.com.clientefacil.core.config.RabbitMQConfig;
import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumer da fila: persiste a notificação (via NotificationService) e, em seguida,
 * empurra o resultado já salvo (com id e status definitivos) só para a sessão STOMP do
 * usuário destinatário — convertAndSendToUser roteia usando o StompPrincipal(userId)
 * associado à conexão no handshake (ver StompAuthChannelInterceptor).
 * <p>
 * SIMULATED_FAILURE_MESSAGE: sentinela usado por NotificationDeadLetterService.simulateNotificationFailure
 * pra testar o pipeline de retry+DLQ+alerta sob demanda (painel admin/dead-letters), sem depender de
 * um erro real acontecer. Checado antes de qualquer persistência, pra não deixar rastro de negócio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    public static final String NOTIFICATION_DESTINATION = "/queue/notifications";
    public static final String SIMULATED_FAILURE_MESSAGE = "__SIMULATE_DLQ_FAILURE__";

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receive(NotificationMessageDTO message) {
        if (SIMULATED_FAILURE_MESSAGE.equals(message.message())) {
            throw new RuntimeException("Falha simulada para testar o pipeline de DLQ (notificação)");
        }

        NotificationResponse notification = notificationService.create(message);

        log.info("Notificação {} persistida para o usuário {}", notification.id(), notification.userId());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(notification.userId()),
                NOTIFICATION_DESTINATION,
                notification
        );
    }
}
