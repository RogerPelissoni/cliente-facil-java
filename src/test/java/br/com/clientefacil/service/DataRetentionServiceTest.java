package br.com.clientefacil.service;

import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import br.com.clientefacil.repository.NotificationRepository;
import br.com.clientefacil.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cada etapa de limpeza (notificação lida, dead-letter resolvido, token gasto/expirado) — o que
 * importa aqui é o threshold calculado a partir dos dias de retenção configurados, e que a query
 * certa é chamada em cada uma. Não testa o agendamento em si (`@Scheduled`), só o comportamento
 * quando disparado.
 */
@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    private static final int NOTIFICATION_DAYS = 365;
    private static final int DEAD_LETTER_DAYS = 90;
    private static final int USER_TOKEN_DAYS = 30;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeadLetterRepository notificationDeadLetterRepository;
    @Mock
    private UserTokenRepository userTokenRepository;

    private DataRetentionService service;

    @BeforeEach
    void setUp() {
        service = new DataRetentionService(
                notificationRepository,
                notificationDeadLetterRepository,
                userTokenRepository,
                NOTIFICATION_DAYS,
                DEAD_LETTER_DAYS,
                USER_TOKEN_DAYS
        );
    }

    @Test
    void purgeOldReadNotifications_usesAThresholdBasedOnTheConfiguredRetentionDays() {
        when(notificationRepository.deleteByDtReadBefore(any())).thenReturn(3L);

        service.purgeOldReadNotifications();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteByDtReadBefore(captor.capture());
        assertThat(captor.getValue())
                .isCloseTo(LocalDateTime.now().minusDays(NOTIFICATION_DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeOldResolvedDeadLetters_usesAThresholdBasedOnTheConfiguredRetentionDays() {
        when(notificationDeadLetterRepository.deleteByDtResolvedBefore(any())).thenReturn(1L);

        service.purgeOldResolvedDeadLetters();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationDeadLetterRepository).deleteByDtResolvedBefore(captor.capture());
        assertThat(captor.getValue())
                .isCloseTo(LocalDateTime.now().minusDays(DEAD_LETTER_DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeSpentUserTokens_usesTheSameThreshold_forBothUsedAndExpired() {
        when(userTokenRepository.deleteByDtUsedAtBeforeOrDtExpiresAtBefore(any(), any())).thenReturn(2L);

        service.purgeSpentUserTokens();

        ArgumentCaptor<LocalDateTime> usedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userTokenRepository).deleteByDtUsedAtBeforeOrDtExpiresAtBefore(usedCaptor.capture(), expiresCaptor.capture());

        assertThat(usedCaptor.getValue()).isEqualTo(expiresCaptor.getValue());
        assertThat(usedCaptor.getValue())
                .isCloseTo(LocalDateTime.now().minusDays(USER_TOKEN_DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeExpiredData_runsAllThreeCleanupSteps() {
        service.purgeExpiredData();

        verify(notificationRepository).deleteByDtReadBefore(any());
        verify(notificationDeadLetterRepository).deleteByDtResolvedBefore(any());
        verify(userTokenRepository).deleteByDtUsedAtBeforeOrDtExpiresAtBefore(any(), any());
    }
}
