package br.com.clientefacil.controller;

import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.dto.CompanyScreenResponse;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.service.CompanyService;
import br.com.clientefacil.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;
    private final PersonService personService;

    @Operation(summary = "SCREEN")
    @PostMapping("/screen")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public CompanyScreenResponse screen(@RequestBody DefaultSearchRequest request) {
        Page<CompanyResponse> obCompanies = service.search(request);
        Map<Long, String> kvPerson = personService.keyValue();

        return new CompanyScreenResponse(obCompanies, kvPerson);
    }

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public Page<CompanyResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public Page<CompanyResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public CompanyResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('COMPANY_CREATE')")
    public CompanyResponse create(@RequestBody @Valid CompanyRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public CompanyResponse update(
            @PathVariable Long id,
            @RequestBody @Valid CompanyRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COMPANY_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}