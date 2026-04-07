package br.com.clientefacil.service;

import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.entity.Person;
import br.com.clientefacil.mapper.PersonMapper;
import br.com.clientefacil.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService extends CoreService<Person, PersonResponse, PersonMapper, Long> {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    @Override
    protected JpaRepository<Person, Long> getRepository() {
        return repository;
    }

    @Override
    protected PersonMapper getMapper() {
        return mapper;
    }

    @Override
    protected String getEntityName() {
        return "Pessoa";
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

    private void fillEntityByRequest(Person entity, PersonRequest request) {
        entity.setName(request.name());
        entity.setDsDocument(request.dsDocument());
        entity.setTpGender(request.tpGender());
        entity.setFlActive(request.flActive());
    }
}