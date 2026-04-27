package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.*;
import br.com.clientefacil.entity.Person;
import br.com.clientefacil.entity.PersonAddress;
import br.com.clientefacil.entity.PersonMail;
import br.com.clientefacil.entity.PersonPhone;
import br.com.clientefacil.mapper.PersonAddressMapper;
import br.com.clientefacil.mapper.PersonMailMapper;
import br.com.clientefacil.mapper.PersonMapper;
import br.com.clientefacil.mapper.PersonPhoneMapper;
import br.com.clientefacil.repository.PersonAddressRepository;
import br.com.clientefacil.repository.PersonMailRepository;
import br.com.clientefacil.repository.PersonPhoneRepository;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.search.PersonSearchConfig;
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
public class PersonService {

    private final PersonRepository repository;
    private final PersonPhoneRepository personPhoneRepository;
    private final PersonAddressRepository personAddressRepository;
    private final PersonMailRepository personMailRepository;

    private final PersonMapper mapper;
    private final PersonPhoneMapper personPhoneMapper;
    private final PersonAddressMapper personAddressMapper;
    private final PersonMailMapper personMailMapper;
    private final PersonMapper personMapper;

    public Page<PersonResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, PersonSearchConfig.SORT_FIELDS)
        );

        Specification<Person> specification = PersonSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<PersonResponse> findAll(
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

    public PersonWithRelationsResponse findByIdWithRelations(Long id) {
        return mapper.toResponseWithRelations(
                repository.findByIdWithRelations(id)
                        .orElseThrow(() -> new RuntimeException("Pessoa não encontrada"))
        );
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
        Person entity = personMapper.toEntity(request);
        entity = repository.save(entity);

        syncRelations(request, entity);

        return mapper.toResponse(entity);
    }

    public PersonResponse update(Long id, PersonRequest request) {
        Person entity = findEntityById(id);
        personMapper.updateEntityFromRequest(request, entity);
        repository.save(entity);

        syncRelations(request, entity);

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Person findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
    }

    private void syncRelations(PersonRequest request, Person person) {

        for (PersonAddressRequest personAddressRequest : request.personAddresses()) {
            PersonAddress personAddress = personAddressMapper.toEntity(personAddressRequest);
            personAddress.setPerson(person);
            personAddressRepository.save(personAddress);
        }

        for (PersonPhoneRequest personPhoneRequest : request.personPhones()) {
            PersonPhone personPhone = personPhoneMapper.toEntity(personPhoneRequest);
            personPhone.setPerson(person);
            personPhoneRepository.save(personPhone);
        }

        for (PersonMailRequest personMailRequest : request.personMails()) {
            PersonMail personMail = personMailMapper.toEntity(personMailRequest);
            personMail.setPerson(person);
            personMailRepository.save(personMail);
        }
    }
}