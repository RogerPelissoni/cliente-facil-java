package br.com.clientefacil.dto;

public record PersonMailResponse(
        Long id,
        String dsMail,
        Boolean flMain
) {
}
