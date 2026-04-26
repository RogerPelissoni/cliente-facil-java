package br.com.clientefacil.controller;

import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public Page<PersonResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public Page<PersonResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public PersonResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('PERSON_CREATE')")
    public PersonResponse create(@RequestBody @Valid PersonRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSON_UPDATE')")
    public PersonResponse update(
            @PathVariable Long id,
            @RequestBody @Valid PersonRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERSON_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}