package br.com.clientefacil.controller;

import br.com.clientefacil.dto.EventMessageRequest;
import br.com.clientefacil.dto.EventMessageResponse;
import br.com.clientefacil.service.EventMessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eventMessage")
@RequiredArgsConstructor
public class EventMessageController {

    private final EventMessageService service;

    @Operation(summary = "FIND_BY_EVENT")
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public List<EventMessageResponse> findByEvent(@PathVariable Long eventId) {
        return service.findByEvent(eventId);
    }

    @Operation(summary = "FIND_BY_ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public EventMessageResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "CREATE")
    @PostMapping
    @PreAuthorize("hasAuthority('EVENT_CREATE')")
    public EventMessageResponse create(@RequestBody @Valid EventMessageRequest request) {
        return service.create(request);
    }

    @Operation(summary = "UPDATE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_UPDATE')")
    public EventMessageResponse update(
            @PathVariable Long id,
            @RequestBody @Valid EventMessageRequest request
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