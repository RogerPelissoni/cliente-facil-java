package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.NotificationDeadLetterResponse;
import br.com.clientefacil.dto.NotificationDeadLetterStatsResponse;
import br.com.clientefacil.entity.NotificationDeadLetter;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import br.com.clientefacil.mapper.NotificationDeadLetterMapper;
import br.com.clientefacil.messaging.EmailMessageDTO;
import br.com.clientefacil.messaging.EmailListener;
import br.com.clientefacil.messaging.EmailPublisher;
import br.com.clientefacil.messaging.NotificationListener;
import br.com.clientefacil.messaging.NotificationMessageDTO;
import br.com.clientefacil.messaging.NotificationPublisher;
import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import br.com.clientefacil.search.NotificationDeadLetterSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDeadLetterService {

    // Destinatário do e-mail de teste simulado: nunca chega a ser usado de verdade, já que
    // EmailListener lança a falha simulada antes de resolver config ou tentar enviar qualquer coisa.
    private static final String SIMULATE_EMAIL_FAILURE_RECIPIENT = "dlq-test@clientefacil.local";

    private final NotificationDeadLetterRepository repository;
    private final NotificationDeadLetterMapper mapper;
    private final NotificationPublisher notificationPublisher;
    private final EmailPublisher emailPublisher;

    public Page<NotificationDeadLetterResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, NotificationDeadLetterSearchConfig.SORT_FIELDS)
        );

        Specification<NotificationDeadLetter> specification =
                NotificationDeadLetterSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public NotificationDeadLetterStatsResponse stats() {
        long totalPending = repository.countByDtResolvedIsNull();
        long totalResolved = repository.countByDtResolvedIsNotNull();
        long totalLast24h = repository.countByCreatedAtAfter(LocalDateTime.now().minusHours(24));

        return new NotificationDeadLetterStatsResponse(totalPending, totalResolved, totalLast24h);
    }

    public NotificationDeadLetterResponse resolve(Long id) {
        NotificationDeadLetter entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de dead letter não encontrado"));

        if (entity.getDtResolved() == null) {
            entity.setDtResolved(LocalDateTime.now());
            repository.save(entity);
        }

        return mapper.toResponse(entity);
    }

    // Publica uma notificação com a sentinela que NotificationListener reconhece e rejeita de
    // propósito — usado pelo painel admin/dead-letters pra provar, sob demanda, que o retry+DLQ+
    // alerta de notificação está funcionando de ponta a ponta (sem esperar uma falha real acontecer).
    public void simulateNotificationFailure() {
        Long userId = SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new AccessDeniedException("Não autenticado"));

        notificationPublisher.publish(new NotificationMessageDTO(
                userId, NotificationTypeEnum.ERROR, "Teste de DLQ", NotificationListener.SIMULATED_FAILURE_MESSAGE));
    }

    // Mesma ideia, só que pro pipeline de e-mail: EmailListener vê a variável simulateFailure e
    // rejeita antes de tentar SMTP de verdade.
    public void simulateEmailFailure() {
        emailPublisher.publish(new EmailMessageDTO(
                null, List.of(SIMULATE_EMAIL_FAILURE_RECIPIENT), "Teste de DLQ", "test-email",
                Map.of(EmailListener.SIMULATE_FAILURE_VARIABLE, true)));
    }
}
