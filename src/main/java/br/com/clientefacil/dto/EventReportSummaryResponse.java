package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;

import java.util.Map;

// Agregados sobre todo o conjunto filtrado, não só a página atual.
public record EventReportSummaryResponse(
        long totalEvents,
        double totalValue,
        Map<EventStatusEnum, Long> countByStatus,
        Map<EventTypeEnum, Long> countByType
) {
}
