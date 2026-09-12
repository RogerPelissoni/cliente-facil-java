package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;

import java.time.LocalDateTime;

public record EventReportItemResponse(
        Long id,
        String dsTitle,
        LocalDateTime dtStart,
        LocalDateTime dtEnd,
        EventStatusEnum tpStatus,
        EventTypeEnum tpEvent,
        Long clientId,
        String clientName,
        Long professionalId,
        String professionalName,
        Double vlTotal,
        Long ownerId,
        String ownerName
) {
}
