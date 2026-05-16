package br.com.clientefacil.dto;

public record ClientResponse(
        Long id,
        Long personId,
        String personName
) {
}
