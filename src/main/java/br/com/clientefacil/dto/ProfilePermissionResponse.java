package br.com.clientefacil.dto;

public record ProfilePermissionResponse(
        Long id,
        Long resourceId,
        String resourceName,
        String resourceSignature,
        Long moduleId,
        String moduleName,
        boolean hasPermission
) {
}