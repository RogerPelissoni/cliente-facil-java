package br.com.clientefacil.controller;

import br.com.clientefacil.dto.ClientRequest;
import br.com.clientefacil.dto.ClientResponse;
import br.com.clientefacil.dto.ClientScreenResponse;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.service.ClientService;
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
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;
    private final PersonService personService;

    @Operation(summary = "SCREEN")
    @PostMapping("/screen")
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    public ClientScreenResponse screen(@RequestBody DefaultSearchRequest request) {
        Page<ClientResponse> obCompanies = service.search(request);
        Map<Long, String> kvPerson = personService.keyValue();

        return new ClientScreenResponse(obCompanies, kvPerson);
    }

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    public Page<ClientResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    public Page<ClientResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    public ClientResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE')")
    public ClientResponse create(@RequestBody @Valid ClientRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    public ClientResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ClientRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CLIENT_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}