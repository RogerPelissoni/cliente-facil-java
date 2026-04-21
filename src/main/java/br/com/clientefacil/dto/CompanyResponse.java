package br.com.clientefacil.dto;

public record CompanyResponse(
        Long id,
        String name,

        Long personId,

        String personName
) {
}
