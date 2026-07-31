package br.com.clientefacil.core.dto;

public record AuthenticatedUserResponse(
        Long id,
        String email
) {
}
