package br.com.clientefacil.core.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(
        @NotBlank String token
) {
}
