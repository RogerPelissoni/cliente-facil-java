package br.com.clientefacil.messaging;

import java.time.LocalDateTime;

// Payload transmitido via WebSocket (broadcast) quando uma mensagem cai na DLQ. Só metadados —
// não inclui o conteúdo original da notificação (título/mensagem), que pode ser sensível e
// pertence a um usuário específico, não a quem estiver de olho no canal de alertas.
public record NotificationDeadLetterAlert(
        Long id,
        String reason,
        Integer deathCount,
        LocalDateTime failedAt
) {
}
