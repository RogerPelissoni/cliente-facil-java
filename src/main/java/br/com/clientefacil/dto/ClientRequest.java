package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotNull Long personId
) {
}