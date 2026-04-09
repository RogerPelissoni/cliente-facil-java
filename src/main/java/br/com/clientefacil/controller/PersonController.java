package br.com.clientefacil.controller;

import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public List<PersonResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public Page<PersonResponse> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAllPaged(page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSON_VIEW')")
    public PersonResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERSON_CREATE')")
    public PersonResponse create(@RequestBody @Valid PersonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERSON_UPDATE')")
    public PersonResponse update(
            @PathVariable Long id,
            @RequestBody @Valid PersonRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERSON_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}