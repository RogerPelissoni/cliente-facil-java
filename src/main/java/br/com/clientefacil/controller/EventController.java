package br.com.clientefacil.controller;

import br.com.clientefacil.dto.*;
import br.com.clientefacil.service.ClientService;
import br.com.clientefacil.service.EventService;
import br.com.clientefacil.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;
    private final ClientService clientService;
    private final ProfessionalService professionalService;

    @Operation(summary = "SCREEN")
    @PostMapping("/screen")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public EventScreenResponse screen() {
        List<EventResponse> events = service.findByAuthUser();
        Map<Long, String> kvClient = clientService.keyValue();
        Map<Long, String> kvProfessional = professionalService.keyValue();

        return new EventScreenResponse(events, kvClient, kvProfessional);
    }

    @Operation(summary = "SEARCH")
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public Page<EventResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @Operation(summary = "FIND_ALL")
    @GetMapping
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public Page<EventResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @Operation(summary = "FIND_BY_AUTH_USER")
    @GetMapping("/findByAuthUser")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public List<EventResponse> findByAuthUser() {
        return service.findByAuthUser();
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public EventWithRelationsResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('EVENT_CREATE')")
    public EventResponse create(@RequestBody @Valid EventRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_UPDATE')")
    public EventResponse update(
            @PathVariable Long id,
            @RequestBody @Valid EventRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "DELETE")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EVENT_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}