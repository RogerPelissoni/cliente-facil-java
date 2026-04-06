package br.com.clientefacil.controller;

import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;

    @GetMapping
    public List<CompanyResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public CompanyResponse create(@RequestBody @Valid CompanyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(
            @PathVariable Long id,
            @RequestBody @Valid CompanyRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}