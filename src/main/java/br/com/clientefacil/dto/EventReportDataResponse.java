package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

public record EventReportDataResponse(
        EventReportSummaryResponse summary,
        Page<EventReportItemResponse> obEvent
) {
}
