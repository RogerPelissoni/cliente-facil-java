package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record CompanyScreenResponse(
        Page<CompanyResponse> obCompanies,
        Map<Long, String> kvPerson
) {
}
