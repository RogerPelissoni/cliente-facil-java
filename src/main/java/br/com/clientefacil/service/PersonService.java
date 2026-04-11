package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.entity.Person;
import br.com.clientefacil.mapper.PersonMapper;
import br.com.clientefacil.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    public List<PersonResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    public Page<PersonResponse> findAllPaged(int page, int size, String sort, String direction) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public PersonResponse findById(Long id) {
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

    public PersonResponse create(PersonRequest request) {
        Person entity = new Person();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public PersonResponse update(Long id, PersonRequest request) {
        Person entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Person findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
    }

    private void fillEntityByRequest(Person entity, PersonRequest request) {
        entity.setName(request.name());
        entity.setDsDocument(request.dsDocument());
        entity.setTpGender(request.tpGender());
        entity.setFlActive(request.flActive());
    }
}