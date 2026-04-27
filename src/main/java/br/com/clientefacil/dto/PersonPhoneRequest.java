package br.com.clientefacil.dto;

public record PersonPhoneRequest(
        Long id,
        String dsPhone,
        Boolean flMain
) {
}
