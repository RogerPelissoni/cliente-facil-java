package br.com.clientefacil.dto;

public record PersonAddressRequest(
        Long id,
        String dsStreet,
        String dsNumber,
        String dsComplement,
        String dsDistrict,
        String dsCity,
        String dsState,
        String dsZipCode,
        Boolean flMain
) {
}
