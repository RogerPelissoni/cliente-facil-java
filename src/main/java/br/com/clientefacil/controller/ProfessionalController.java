package br.com.clientefacil.controller;

import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.ProfessionalRequest;
import br.com.clientefacil.dto.ProfessionalResponse;
import br.com.clientefacil.dto.ProfessionalScreenResponse;
import br.com.clientefacil.service.PersonService;
import br.com.clientefacil.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/professional")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService service;
    private final PersonService personService;

    @Operation(summary = "SCREEN")
    @PostMapping("/screen")
    @PreAuthorize("hasAuthority('PROFESSIONAL_VIEW')")
    public ProfessionalScreenResponse screen(@RequestBody DefaultSearchRequest request) {
        Page<ProfessionalResponse> obProfessionals = service.search(request);
        Map<Long, String> kvPerson = personService.keyValue();

        return new ProfessionalScreenResponse(obProfessionals, kvPerson);
    }

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('PROFESSIONAL_VIEW')")
    public Page<ProfessionalResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('PROFESSIONAL_VIEW')")
    public Page<ProfessionalResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFESSIONAL_VIEW')")
    public ProfessionalResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('PROFESSIONAL_CREATE')")
    public ProfessionalResponse create(@RequestBody @Valid ProfessionalRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFESSIONAL_UPDATE')")
    public ProfessionalResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ProfessionalRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROFESSIONAL_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}