package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record EventReportResponse(
        EventReportSummaryResponse summary,
        Page<EventReportItemResponse> obEvent,
        Map<Long, String> kvClient,
        Map<Long, String> kvProfessional
) {
}
