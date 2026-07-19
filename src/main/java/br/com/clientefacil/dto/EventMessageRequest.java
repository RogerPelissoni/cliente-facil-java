package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record EventMessageRequest(
        @NotNull Long eventId,
        @NotNull String dsMessage
) {
}
