package br.com.clientefacil.controller;

import br.com.clientefacil.core.CoreController;
import br.com.clientefacil.core.CoreCrudService;
import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
public class PersonController extends CoreController<PersonResponse, Long> {

    private final PersonService service;

    @Override
    protected CoreCrudService<PersonResponse, Long> getService() {
        return service;
    }

    @PostMapping
    public PersonResponse create(@RequestBody @Valid PersonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PersonResponse update(
            @PathVariable Long id,
            @RequestBody @Valid PersonRequest request
    ) {
        return service.update(id, request);
    }
}