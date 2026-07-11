package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.EventRequest;
import br.com.clientefacil.dto.EventResponse;
import br.com.clientefacil.entity.Event;
import br.com.clientefacil.entity.EventOwner;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.mapper.EventMapper;
import br.com.clientefacil.repository.EventOwnerRepository;
import br.com.clientefacil.repository.EventRepository;
import br.com.clientefacil.repository.UserRepository;
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

    public EventResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
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

        return mapper.toResponse(entity);
    }

    public EventResponse update(Long id, EventRequest request) {
        Event entity = findEntityById(id);
        mapper.updateEntityFromRequest(request, entity);
        repository.save(entity);

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
}