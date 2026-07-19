package br.com.clientefacil.dto;

public record EventMessageResponse(
        Long id,
        Long eventId,
        String dsMessage
) {
}
