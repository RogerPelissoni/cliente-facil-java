package br.com.clientefacil.service;

import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.entity.Company;
import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.mapper.CompanyMapper;
import br.com.clientefacil.repository.CompanyRepository;
import br.com.clientefacil.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repository;
    private final PersonRepository personRepository;
    private final CompanyMapper mapper;

    public List<CompanyResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    public Page<CompanyResponse> findAllPaged(int page, int size, String sort, String direction) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public CompanyResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public CompanyResponse create(CompanyRequest request) {
        Company entity = new Company();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Company findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    private void fillEntityByRequest(Company entity, CompanyRequest request) {
        var person = personRepository.getReferenceById(request.personId());

        entity.setName(request.name());
        entity.setPerson(person);
    }
}