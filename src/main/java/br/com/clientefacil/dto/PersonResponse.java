package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.PersonGenderEnum;

public record PersonResponse(
        Long id,
        String name,
        String dsDocument,
        PersonGenderEnum tpGender,
        Boolean flActive
) {
}
