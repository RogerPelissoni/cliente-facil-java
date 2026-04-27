package br.com.clientefacil.dto;

public record PersonMailRequest(
        Long id,
        String dsMail,
        Boolean flMain
) {
}
