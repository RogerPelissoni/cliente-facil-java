package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        String phone,
        String email
) {
}