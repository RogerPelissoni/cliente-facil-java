package br.com.clientefacil.core.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emite tickets efêmeros e de uso único para autenticar o handshake STOMP, no lugar de expor o
 * JWT completo (de 24h) ao JS do navegador — ver StompAuthChannelInterceptor e o endpoint
 * GET /api/v1/auth/ws-ticket em AuthController.
 * <p>
 * Guardado em memória (não no banco) de propósito: é um artefato de curtíssima duração, só
 * relevante para esta instância do processo, e não precisa sobreviver a um restart.
 * Não escala para múltiplas instâncias do backend atrás de um load balancer sem sticky sessions
 * (o ticket só existe na instância que o emitiu) — aceitável para o porte atual do projeto.
 */
@Service
public class WsTicketService {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        evictExpired();

        String value = UUID.randomUUID().toString();
        tickets.put(value, new Ticket(userId, Instant.now().plus(TTL)));

        return value;
    }

    public Optional<Long> consume(String value) {
        Ticket ticket = tickets.remove(value);

        if (ticket == null || Instant.now().isAfter(ticket.expiresAt())) {
            return Optional.empty();
        }

        return Optional.of(ticket.userId());
    }

    private void evictExpired() {
        Instant now = Instant.now();
        tickets.values().removeIf(ticket -> now.isAfter(ticket.expiresAt()));
    }

    private record Ticket(Long userId, Instant expiresAt) {
    }
}
