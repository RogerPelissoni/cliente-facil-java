package br.com.clientefacil.service;

import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.entity.Company;
import br.com.clientefacil.mapper.CompanyMapper;
import br.com.clientefacil.repository.CompanyRepository;
import br.com.clientefacil.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
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

    public CompanyResponse findById(Long id) {
        return mapper.toResponse(findCompanyById(id));
    }

    public CompanyResponse create(CompanyRequest request) {
        var person = personRepository.getReferenceById(request.personId());

        Company company = new Company();
        company.setName(request.name());
        company.setPerson(person);
        Company saved = repository.save(company);

        return mapper.toResponse(saved);
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findCompanyById(id);
        var person = personRepository.getReferenceById(request.personId());

        company.setName(request.name());
        company.setPerson(person);

        Company updated = repository.save(company);

        return mapper.toResponse(updated);
    }

    public void delete(Long id) {
        repository.delete(findCompanyById(id));
    }

    private Company findCompanyById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa nao encontrado"));
    }
}
