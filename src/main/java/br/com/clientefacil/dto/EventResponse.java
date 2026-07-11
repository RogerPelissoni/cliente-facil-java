package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String dsTitle,
        String dsDescription,
        LocalDateTime dtStart,
        LocalDateTime dtEnd,
        EventStatusEnum tpStatus,
        EventTypeEnum tpEvent
) {
}
