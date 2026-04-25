package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

public record ProfilePermissionRequest(
        Long id,
        @NotNull Long moduleId,
        @NotNull Long resourceId,
        @NotNull String resourceSignature,
        @NotNull Boolean hasPermission
) {
}