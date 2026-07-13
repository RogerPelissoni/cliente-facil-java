package br.com.clientefacil.dto;

public record EventServiceResponse(
        Long id,
        Long clientId,
        Long professionalId,
        Long accountReceivableId
) {
}