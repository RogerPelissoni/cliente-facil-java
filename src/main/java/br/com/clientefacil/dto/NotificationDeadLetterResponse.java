package br.com.clientefacil.dto;

import java.time.LocalDateTime;

public record NotificationDeadLetterResponse(
        Long id,
        String dsPayload,
        String dsErrorReason,
        Integer nrDeathCount,
        LocalDateTime dtFailedAt,
        LocalDateTime dtResolved,
        LocalDateTime createdAt
) {
}
