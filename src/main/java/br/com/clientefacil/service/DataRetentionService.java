package br.com.clientefacil.service;

import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import br.com.clientefacil.repository.NotificationRepository;
import br.com.clientefacil.repository.UserTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Limpeza automática do que já é lixo operacional — nenhuma das três tabelas abaixo tinha rotina de
 * limpeza antes disso, então cresciam pra sempre (`notification`, `notification_dead_letter`,
 * `user_token`). Só apaga o que já está "resolvido" de algum jeito (lido/resolvido/usado ou
 * expirado) — nunca algo ainda pendente de ação.
 * <p>
 * Roda sem usuário autenticado (job em background) — {@code TenantFilterAspect} não habilita o
 * filtro de tenant nesse caso (nenhum {@code SecurityUtil.getAuthenticatedUser()}), então as
 * queries alcançam todas as empresas de propósito: é limpeza de infraestrutura, não uma operação de
 * negócio de uma empresa só.
 */
@Slf4j
@Service
public class DataRetentionService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeadLetterRepository notificationDeadLetterRepository;
    private final UserTokenRepository userTokenRepository;

    private final int notificationRetentionDays;
    private final int deadLetterRetentionDays;
    private final int userTokenRetentionDays;

    public DataRetentionService(
            NotificationRepository notificationRepository,
            NotificationDeadLetterRepository notificationDeadLetterRepository,
            UserTokenRepository userTokenRepository,
            @Value("${app.data-retention.notification-days}") int notificationRetentionDays,
            @Value("${app.data-retention.dead-letter-days}") int deadLetterRetentionDays,
            @Value("${app.data-retention.user-token-days}") int userTokenRetentionDays
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationDeadLetterRepository = notificationDeadLetterRepository;
        this.userTokenRepository = userTokenRepository;
        this.notificationRetentionDays = notificationRetentionDays;
        this.deadLetterRetentionDays = deadLetterRetentionDays;
        this.userTokenRetentionDays = userTokenRetentionDays;
    }

    @Scheduled(cron = "${app.data-retention.cron}")
    public void purgeExpiredData() {
        purgeOldReadNotifications();
        purgeOldResolvedDeadLetters();
        purgeSpentUserTokens();
    }

    // package-private (não private) só pra permitir teste unitário direto de cada etapa, sem
    // precisar disparar o método @Scheduled inteiro — mesmo motivo de outras relaxações de
    // visibilidade nesta base de código (ver DeathInfo em NotificationDeadLetterListener).
    void purgeOldReadNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(notificationRetentionDays);
        long deleted = notificationRepository.deleteByDtReadBefore(threshold);

        if (deleted > 0) {
            log.info("Limpeza de dados: {} notificação(ões) lida(s) há mais de {} dias removida(s)",
                    deleted, notificationRetentionDays);
        }
    }

    void purgeOldResolvedDeadLetters() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(deadLetterRetentionDays);
        long deleted = notificationDeadLetterRepository.deleteByDtResolvedBefore(threshold);

        if (deleted > 0) {
            log.info("Limpeza de dados: {} dead-letter(s) resolvido(s) há mais de {} dias removido(s)",
                    deleted, deadLetterRetentionDays);
        }
    }

    void purgeSpentUserTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(userTokenRetentionDays);
        long deleted = userTokenRepository.deleteByDtUsedAtBeforeOrDtExpiresAtBefore(threshold, threshold);

        if (deleted > 0) {
            log.info("Limpeza de dados: {} token(s) gasto(s)/expirado(s) há mais de {} dias removido(s)",
                    deleted, userTokenRetentionDays);
        }
    }
}
