package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record EventServiceRequest(
        @NotNull Long clientId,
        @NotNull Long professionalId
) {
}