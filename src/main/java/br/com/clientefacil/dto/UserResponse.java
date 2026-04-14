package br.com.clientefacil.dto;

public record UserResponse(
        Long id,
        String name,
        String email,

        Long personId,
        Long profileId,
        Long companyId,

        String personName,
        String profileName,
        String companyName
) {
}