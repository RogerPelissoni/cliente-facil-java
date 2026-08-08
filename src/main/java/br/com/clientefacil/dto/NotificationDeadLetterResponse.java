package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.DeadLetterOriginEnum;

import java.time.LocalDateTime;

public record NotificationDeadLetterResponse(
        Long id,
        DeadLetterOriginEnum tpOrigin,
        String dsPayload,
        String dsErrorReason,
        Integer nrDeathCount,
        LocalDateTime dtFailedAt,
        LocalDateTime dtResolved,
        LocalDateTime createdAt
) {
}
