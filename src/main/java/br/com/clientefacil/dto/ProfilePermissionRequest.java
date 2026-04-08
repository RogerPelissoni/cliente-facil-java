package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record ProfilePermissionRequest(
        @NotNull Long idResource,
        @NotNull Short nrPermissionLevel
) {
}