package br.com.clientefacil.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,

        Long personId,
        Long profileId,
        Long companyId,

        String personName,
        String profileName,
        String companyName,

        LocalDateTime dtEmailConfirmedAt
) {
}