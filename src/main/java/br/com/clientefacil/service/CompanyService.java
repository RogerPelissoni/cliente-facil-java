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
public class CompanyService extends CoreService<Company, CompanyResponse, Long> {

    private final CompanyRepository repository;
    private final PersonRepository personRepository;
    private final CompanyMapper mapper;

    @Override
    protected JpaRepository<Company, Long> getRepository() {
        return repository;
    }

    @Override
    protected CompanyResponse toResponse(Company entity) {
        return mapper.toResponse(entity);
    }

    public CompanyResponse create(CompanyRequest request) {
        var person = personRepository.getReferenceById(request.personId());

        Company company = new Company();
        company.setName(request.name());
        company.setPerson(person);

        return mapper.toResponse(repository.save(company));
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findEntityById(id);
        var person = personRepository.getReferenceById(request.personId());

        company.setName(request.name());
        company.setPerson(person);

        return mapper.toResponse(repository.save(company));
    }
}