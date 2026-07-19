package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.dto.EventMessageRequest;
import br.com.clientefacil.dto.EventMessageResponse;
import br.com.clientefacil.entity.EventMessage;
import br.com.clientefacil.mapper.EventMessageMapper;
import br.com.clientefacil.repository.EventMessageRepository;
import br.com.clientefacil.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventMessageService {

    private final EventMessageRepository repository;
    private final EventRepository eventRepository;
    private final EventMessageMapper mapper;

    public List<EventMessageResponse> findByEvent(Long idEvent) {
        return repository.findEntityByEventId(idEvent)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public EventMessageResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public EventMessageResponse create(EventMessageRequest request) {
        EventMessage entity = new EventMessage();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public EventMessageResponse update(Long id, EventMessageRequest request) {
        EventMessage entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private EventMessage findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem não encontrada"));
    }

    private void fillEntityByRequest(EventMessage entity, EventMessageRequest request) {
        var event = eventRepository.getReferenceById(request.eventId());
        entity.setEvent(event);
        entity.setDsMessage(request.dsMessage());
    }
}