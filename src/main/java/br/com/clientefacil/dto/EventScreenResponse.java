package br.com.clientefacil.dto;

import java.util.List;
import java.util.Map;

public record EventScreenResponse(
        List<EventResponse> obEvent,
        Map<Long, String> kvClient,
        Map<Long, String> kvProfessional
) {
}
