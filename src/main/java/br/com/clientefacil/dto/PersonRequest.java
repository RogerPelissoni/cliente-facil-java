package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.PersonGenderEnum;
import jakarta.validation.constraints.NotBlank;

public record PersonRequest(
        @NotBlank String name,
        @NotBlank String dsDocument,
        PersonGenderEnum tpGender,
        Boolean flActive
) {
}