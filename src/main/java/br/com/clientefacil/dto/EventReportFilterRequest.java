package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;

import java.time.LocalDateTime;

public record EventReportFilterRequest(
        LocalDateTime dtStart,
        LocalDateTime dtEnd,
        EventStatusEnum tpStatus,
        EventTypeEnum tpEvent,
        Long clientId,
        Long professionalId,
        Long ownerId,
        Integer page,
        Integer size
) {
    public int pageOrDefault() {
        return page != null ? page : 0;
    }

    public int sizeOrDefault() {
        return size != null ? size : 10;
    }
}
