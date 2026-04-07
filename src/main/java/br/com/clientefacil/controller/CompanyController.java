package br.com.clientefacil.controller;

import br.com.clientefacil.core.CoreController;
import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.CompanyRequest;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController extends CoreController<CompanyResponse, Long> {

    private final CompanyService service;

    @Override
    protected CoreService<?, CompanyResponse, Long> getService() {
        return service;
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
}