package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyRequest(
        @NotBlank String name,
        @NotNull Long personId
) {
}