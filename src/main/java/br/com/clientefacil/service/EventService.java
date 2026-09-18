package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.domain.config.ResourceEnum;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.EventReportDataResponse;
import br.com.clientefacil.dto.EventReportFilterRequest;
import br.com.clientefacil.dto.EventReportItemResponse;
import br.com.clientefacil.dto.EventReportSummaryResponse;
import br.com.clientefacil.dto.EventRequest;
import br.com.clientefacil.dto.EventResponse;
import br.com.clientefacil.dto.EventWithRelationsResponse;
import br.com.clientefacil.entity.*;
import br.com.clientefacil.entity.enums.AccountReceivableStatusEnum;
import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;
import br.com.clientefacil.mapper.EventMapper;
import br.com.clientefacil.repository.*;
import br.com.clientefacil.search.EventSearchConfig;
import br.com.clientefacil.validator.AccountReceivableValidator;
import br.com.clientefacil.validator.EventScheduleValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final EventOwnerRepository eventOwnerRepository;
    private final UserRepository userRepository;
    private final EventServiceRepository eventServiceRepository;
    private final AccountReceivableRepository accountReceivableRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final EventMapper mapper;
    private final AccountReceivableValidator accountReceivableValidator;
    private final EventScheduleValidator eventScheduleValidator;

    public Page<EventResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, EventSearchConfig.SORT_FIELDS)
        );

        Specification<Event> specification = EventSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<EventResponse> findAll(
            int page,
            int size,
            String sort,
            String direction
    ) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public List<EventResponse> findByAuthUser() {
        Long userId = SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return repository.findAllByUser(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public EventReportDataResponse report(EventReportFilterRequest request) {
        Long ownerId = resolveReportOwnerId(request.ownerId());

        List<Event> events = repository.findForReport(
                request.dtStart(),
                request.dtEnd(),
                request.tpStatus(),
                request.tpEvent(),
                request.clientId(),
                request.professionalId(),
                ownerId
        );

        Map<Long, ReportOwnerInfo> ownersByEventId = findReportOwners(events);

        EventReportSummaryResponse summary = buildReportSummary(events);
        Page<EventReportItemResponse> page = buildReportPage(
                events,
                ownersByEventId,
                request.pageOrDefault(),
                request.sizeOrDefault()
        );

        return new EventReportDataResponse(summary, page);
    }

    // Sem EVENT_REPORT_VIEW_ALL, ignora ownerId do request e força o usuário autenticado.
    private Long resolveReportOwnerId(Long requestedOwnerId) {
        if (hasReportViewAll()) {
            return requestedOwnerId;
        }

        return SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasReportViewAll() {
        return SecurityUtil.getAuthenticatedUser()
                .map(AuthenticatedUser::getAuthorities)
                .map(authorities -> authorities.stream()
                        .anyMatch(authority -> authority.getAuthority()
                                .equals(ResourceEnum.EVENT_REPORT_VIEW_ALL.getSignature())))
                .orElse(false);
    }

    private Map<Long, ReportOwnerInfo> findReportOwners(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        if (eventIds.isEmpty()) {
            return Map.of();
        }

        return repository.findOwnersByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new ReportOwnerInfo((Long) row[1], (String) row[2]),
                        (first, second) -> first // mais de um owner: fica o primeiro (ver EventRepository#findForReport)
                ));
    }

    private record ReportOwnerInfo(Long userId, String userName) {
    }

    private EventReportSummaryResponse buildReportSummary(List<Event> events) {
        double totalValue = events.stream()
                .map(this::eventAccountReceivable)
                .filter(Objects::nonNull)
                .mapToDouble(AccountReceivable::getVlTotal)
                .sum();

        Map<EventStatusEnum, Long> countByStatus = events.stream()
                .collect(Collectors.groupingBy(Event::getTpStatus, Collectors.counting()));

        Map<EventTypeEnum, Long> countByType = events.stream()
                .collect(Collectors.groupingBy(Event::getTpEvent, Collectors.counting()));

        return new EventReportSummaryResponse(events.size(), totalValue, countByStatus, countByType);
    }

    // Pagina em memória: a lista já vem filtrada e completa de findForReport.
    private Page<EventReportItemResponse> buildReportPage(
            List<Event> events,
            Map<Long, ReportOwnerInfo> ownersByEventId,
            int page,
            int size
    ) {
        List<EventReportItemResponse> items = events.stream()
                .map(event -> toReportItem(event, ownersByEventId.get(event.getId())))
                .toList();

        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());

        return new PageImpl<>(
                items.subList(fromIndex, toIndex),
                PageRequest.of(page, size),
                items.size()
        );
    }

    private EventReportItemResponse toReportItem(Event event, ReportOwnerInfo owner) {
        Client client = eventClient(event);
        Professional professional = eventProfessional(event);
        AccountReceivable accountReceivable = eventAccountReceivable(event);

        return new EventReportItemResponse(
                event.getId(),
                event.getDsTitle(),
                event.getDtStart(),
                event.getDtEnd(),
                event.getTpStatus(),
                event.getTpEvent(),
                client != null ? client.getId() : null,
                client != null ? client.getPerson().getName() : null,
                professional != null ? professional.getId() : null,
                professional != null ? professional.getPerson().getName() : null,
                accountReceivable != null ? accountReceivable.getVlTotal() : null,
                owner != null ? owner.userId() : null,
                owner != null ? owner.userName() : null
        );
    }

    private Client eventClient(Event event) {
        return event.getEventService() != null ? event.getEventService().getClient() : null;
    }

    private Professional eventProfessional(Event event) {
        return event.getEventService() != null ? event.getEventService().getProfessional() : null;
    }

    private AccountReceivable eventAccountReceivable(Event event) {
        return event.getEventService() != null ? event.getEventService().getAccountReceivable() : null;
    }

    public EventWithRelationsResponse findById(Long id) {
        return mapper.toResponseComplete(findEntityById(id));
    }

    public Map<Long, String> keyValue() {
        return repository.keyValue()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
    }

    public EventResponse create(EventRequest request) {
        if (isService(request)) {
            eventScheduleValidator.validateNoConflict(
                    request.eventService().professionalId(),
                    request.dtStart(),
                    request.dtEnd(),
                    null
            );
        }

        Event entity = mapper.toEntity(request);
        entity = repository.save(entity);

        createEventOwner(entity);

        if (isService(request)) {
            br.com.clientefacil.entity.EventService eventService = getOrCreateEventService(entity);
            AccountReceivable accountReceivable = persistAccountReceivable(request, eventService);
            persistEventService(request, eventService, accountReceivable);
        }

        return mapper.toResponse(entity);
    }

    public EventResponse update(Long id, EventRequest request) {
        Event entity = findEntityById(id);

        if (entity.getEventService() != null) {
            accountReceivableValidator.canUpdate(entity.getEventService().getAccountReceivable().getId());
        }

        if (isService(request)) {
            eventScheduleValidator.validateNoConflict(
                    request.eventService().professionalId(),
                    request.dtStart(),
                    request.dtEnd(),
                    id
            );
        }

        mapper.updateEntityFromRequest(request, entity);
        repository.save(entity);

        if (isService(request)) {
            br.com.clientefacil.entity.EventService eventService = getOrCreateEventService(entity);
            AccountReceivable accountReceivable = persistAccountReceivable(request, eventService);
            persistEventService(request, eventService, accountReceivable);
        }

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        Event entity = findEntityById(id);

        if (entity.getEventService() != null) {
            accountReceivableValidator.canDelete(entity.getEventService().getAccountReceivable().getId());
        }

        repository.delete(entity);
    }

    private boolean isService(EventRequest request) {
        return request.tpEvent() == EventTypeEnum.SERVICE;
    }

    private Event findEntityById(Long id) {
        Long userId = SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return repository.findById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrada"));
    }

    private void createEventOwner(Event event) {
        Long authUserId = SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User is not authenticated"));

        User authUser = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("Authenticated User not found"));

        EventOwner eventOwner = new EventOwner();
        eventOwner.setEvent(event);
        eventOwner.setUser(authUser);
        eventOwnerRepository.save(eventOwner);
    }

    private void persistEventService(
            EventRequest request,
            br.com.clientefacil.entity.EventService eventService,
            AccountReceivable accountReceivable
    ) {

        Client client = clientRepository
                .findById(request.eventService().clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Professional professional = professionalRepository
                .findById(request.eventService().professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        eventService.setClient(client);
        eventService.setProfessional(professional);
        eventService.setAccountReceivable(accountReceivable);

        eventServiceRepository.save(eventService);
    }

    private AccountReceivable persistAccountReceivable(
            EventRequest request,
            br.com.clientefacil.entity.EventService eventService
    ) {

        Client client = clientRepository
                .findById(request.eventService().clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        AccountReceivable accountReceivable = eventService.getAccountReceivable();

        if (accountReceivable == null) {

            String lastCode = accountReceivableRepository.findLastCode().orElse("0");
            String nextCode = String.valueOf(Long.parseLong(lastCode) + 1);

            accountReceivable = new AccountReceivable();
            accountReceivable.setDsCode(nextCode);
        }

        accountReceivable.setPerson(client.getPerson());
        accountReceivable.setNrInstallment(1);
        accountReceivable.setVlTotal(request.accountReceivable().vlTotal());
        accountReceivable.setVlBalance(request.accountReceivable().vlTotal());
        accountReceivable.setDaDue(request.accountReceivable().daDue());
        accountReceivable.setTpStatus(AccountReceivableStatusEnum.PENDING);

        return accountReceivableRepository.save(accountReceivable);
    }

    private br.com.clientefacil.entity.EventService getOrCreateEventService(Event event) {

        br.com.clientefacil.entity.EventService eventService = event.getEventService();

        if (eventService == null) {
            eventService = new br.com.clientefacil.entity.EventService();
            eventService.setEvent(event);
        }

        return eventService;
    }
}
