package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.PersonGenderEnum;

public record PersonResponse(
        String name,
        String dsDocument,
        PersonGenderEnum tpGender,
        Boolean flActive
) {
}
