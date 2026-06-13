package br.com.clientefacil.service;

import br.com.clientefacil.core.dto.KeyValueDTO;
import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.ProfessionalRequest;
import br.com.clientefacil.dto.ProfessionalResponse;
import br.com.clientefacil.entity.Professional;
import br.com.clientefacil.mapper.ProfessionalMapper;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfessionalRepository;
import br.com.clientefacil.search.ProfessionalSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository repository;
    private final PersonRepository personRepository;
    private final ProfessionalMapper mapper;

    public Page<ProfessionalResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, ProfessionalSearchConfig.SORT_FIELDS)
        );

        Specification<Professional> specification = ProfessionalSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<ProfessionalResponse> findAll(
            int page,
            int size,
            String sort,
            String direction
    ) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAllWithRelations(pageable)
                .map(mapper::toResponse);
    }

    public ProfessionalResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public Map<Long, String> keyValue() {
        return repository.keyValue()
                .stream()
                .collect(Collectors.toMap(KeyValueDTO::getId, KeyValueDTO::getName));
    }

    public ProfessionalResponse create(ProfessionalRequest request) {
        Professional entity = new Professional();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        Professional entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Professional findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    private void fillEntityByRequest(Professional entity, ProfessionalRequest request) {
        var person = personRepository.getReferenceById(request.personId());
        entity.setPerson(person);
    }
}