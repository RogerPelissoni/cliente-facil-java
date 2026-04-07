package br.com.clientefacil.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String personName,
        String profileName
) {
}
