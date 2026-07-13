package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.EventRequest;
import br.com.clientefacil.dto.EventResponse;
import br.com.clientefacil.dto.EventWithRelationsResponse;
import br.com.clientefacil.entity.*;
import br.com.clientefacil.entity.enums.AccountReceivableStatusEnum;
import br.com.clientefacil.mapper.EventMapper;
import br.com.clientefacil.repository.*;
import br.com.clientefacil.search.EventSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        Event entity = mapper.toEntity(request);
        entity = repository.save(entity);

        createEventOwner(entity);

        br.com.clientefacil.entity.EventService eventService = getOrCreateEventService(entity);
        AccountReceivable accountReceivable = persistAccountReceivable(request, eventService);
        persistEventService(request, eventService, accountReceivable);

        return mapper.toResponse(entity);
    }

    public EventResponse update(Long id, EventRequest request) {
        Event entity = findEntityById(id);

        mapper.updateEntityFromRequest(request, entity);
        repository.save(entity);

        br.com.clientefacil.entity.EventService eventService = getOrCreateEventService(entity);
        AccountReceivable accountReceivable = persistAccountReceivable(request, eventService);
        persistEventService(request, eventService, accountReceivable);

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
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

        br.com.clientefacil.entity.EventService eventService = event.getService();

        if (eventService == null) {
            eventService = new br.com.clientefacil.entity.EventService();
            eventService.setEvent(event);
        }

        return eventService;
    }
}