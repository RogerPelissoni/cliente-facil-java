package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.NotificationDeadLetterResponse;
import br.com.clientefacil.dto.NotificationDeadLetterStatsResponse;
import br.com.clientefacil.entity.NotificationDeadLetter;
import br.com.clientefacil.mapper.NotificationDeadLetterMapper;
import br.com.clientefacil.repository.NotificationDeadLetterRepository;
import br.com.clientefacil.search.NotificationDeadLetterSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationDeadLetterService {

    private final NotificationDeadLetterRepository repository;
    private final NotificationDeadLetterMapper mapper;

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
}
