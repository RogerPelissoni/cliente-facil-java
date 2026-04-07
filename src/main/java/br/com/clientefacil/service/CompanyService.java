package br.com.clientefacil.service;

import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.entity.Company;
import br.com.clientefacil.mapper.CompanyMapper;
import br.com.clientefacil.repository.CompanyRepository;
import br.com.clientefacil.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService extends CoreService<Company, CompanyResponse, CompanyMapper, Long> {

    private final CompanyRepository repository;
    private final PersonRepository personRepository;
    private final CompanyMapper mapper;

    @Override
    protected JpaRepository<Company, Long> getRepository() {
        return repository;
    }

    @Override
    protected CompanyMapper getMapper() {
        return mapper;
    }

    @Override
    protected String getEntityName() {
        return "Empresa";
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

    private void fillEntityByRequest(Company entity, CompanyRequest request) {
        var person = personRepository.getReferenceById(request.personId());

        entity.setName(request.name());
        entity.setPerson(person);
    }
}