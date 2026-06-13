package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record ProfessionalScreenResponse(
        Page<ProfessionalResponse> obProfessionals,
        Map<Long, String> kvPerson
) {
}
