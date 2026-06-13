package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record ProfessionalRequest(
        @NotNull Long personId
) {
}
