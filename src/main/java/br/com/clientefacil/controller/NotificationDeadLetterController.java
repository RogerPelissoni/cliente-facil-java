package br.com.clientefacil.controller;

import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.NotificationDeadLetterResponse;
import br.com.clientefacil.dto.NotificationDeadLetterStatsResponse;
import br.com.clientefacil.service.NotificationDeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/dead-letters")
@RequiredArgsConstructor
public class NotificationDeadLetterController {

    private final NotificationDeadLetterService service;

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('DEAD_LETTER_VIEW')")
    public Page<NotificationDeadLetterResponse> search(@RequestBody DefaultSearchRequest request) {
        return service.search(request);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('DEAD_LETTER_VIEW')")
    public NotificationDeadLetterStatsResponse stats() {
        return service.stats();
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('DEAD_LETTER_RESOLVE')")
    public NotificationDeadLetterResponse resolve(@PathVariable Long id) {
        return service.resolve(id);
    }

    // Dispara uma falha deliberada (via sentinela reconhecida pelo listener correspondente, ver
    // NotificationListener/EmailListener) pra provar, sob demanda, que o pipeline de retry+DLQ+alerta
    // está funcionando de ponta a ponta. Fire-and-forget: assíncrono, leva alguns segundos (retry) até
    // o registro aparecer na listagem — mesmo espírito 202 de /notifications/test.
    @PostMapping("/simulate-failure/notification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('DEAD_LETTER_TEST')")
    public void simulateNotificationFailure() {
        service.simulateNotificationFailure();
    }

    @PostMapping("/simulate-failure/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('DEAD_LETTER_TEST')")
    public void simulateEmailFailure() {
        service.simulateEmailFailure();
    }
}
