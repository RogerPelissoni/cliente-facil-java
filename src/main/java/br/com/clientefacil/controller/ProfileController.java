package br.com.clientefacil.controller;

import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.ProfileRequest;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('PROFILE_VIEW')")
    public Page<ProfileResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('PROFILE_VIEW')")
    public Page<ProfileResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_VIEW')")
    public ProfileResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROFILE_CREATE')")
    public ProfileResponse create(@RequestBody @Valid ProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
    public ProfileResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ProfileRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROFILE_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}