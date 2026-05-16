package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.ClientRequest;
import br.com.clientefacil.dto.ClientResponse;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.entity.Client;
import br.com.clientefacil.mapper.ClientMapper;
import br.com.clientefacil.repository.ClientRepository;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.search.ClientSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repository;
    private final PersonRepository personRepository;

    private final ClientMapper mapper;

    public Page<ClientResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, ClientSearchConfig.SORT_FIELDS)
        );

        Specification<Client> specification = ClientSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<ClientResponse> findAll(
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

    public ClientResponse findById(Long id) {
        return mapper.toResponse(
                repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Pessoa não encontrada"))
        );
    }

    public ClientResponse create(ClientRequest request) {
        Client entity = new Client();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public ClientResponse update(Long id, ClientRequest request) {
        Client entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Client findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
    }

    private void fillEntityByRequest(Client entity, ClientRequest request) {
        var person = personRepository.getReferenceById(request.personId());

        entity.setPerson(person);
    }
}