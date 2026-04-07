package br.com.clientefacil.controller;

import br.com.clientefacil.core.CoreController;
import br.com.clientefacil.core.CoreCrudService;
import br.com.clientefacil.dto.ProfileRequest;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController extends CoreController<ProfileResponse, Long> {

    private final ProfileService service;

    @Override
    protected CoreCrudService<ProfileResponse, Long> getService() {
        return service;
    }

    @PostMapping
    public ProfileResponse create(@RequestBody @Valid ProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProfileResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ProfileRequest request
    ) {
        return service.update(id, request);
    }
}