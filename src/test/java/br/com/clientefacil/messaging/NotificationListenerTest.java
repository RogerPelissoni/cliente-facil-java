package br.com.clientefacil.messaging;

import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.entity.enums.NotificationStatusEnum;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import br.com.clientefacil.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Mesma ideia de EmailListenerTest, pro sentinela usado por "Simular falha (notificação)": precisa
 * rejeitar antes de persistir qualquer coisa ou empurrar via STOMP.
 */
@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationListener(notificationService, messagingTemplate);
    }

    @Test
    void receiveThrowsImmediately_whenMessageIsTheSimulatedFailureSentinel_withoutPersistingOrPushing() {
        NotificationMessageDTO message = new NotificationMessageDTO(
                1L, NotificationTypeEnum.ERROR, "Teste de DLQ", NotificationListener.SIMULATED_FAILURE_MESSAGE);

        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationService, messagingTemplate);
    }

    @Test
    void receivePersistsAndPushesToTheRecipient_whenNotSimulated() {
        NotificationMessageDTO message = new NotificationMessageDTO(7L, NotificationTypeEnum.INFO, "Título", "Corpo");
        NotificationResponse persisted = new NotificationResponse(
                1L, 7L, NotificationTypeEnum.INFO, NotificationStatusEnum.UNREAD, "Título", "Corpo", null, LocalDateTime.now());
        when(notificationService.create(message)).thenReturn(persisted);

        listener.receive(message);

        verify(messagingTemplate).convertAndSendToUser("7", NotificationListener.NOTIFICATION_DESTINATION, persisted);
    }
}
