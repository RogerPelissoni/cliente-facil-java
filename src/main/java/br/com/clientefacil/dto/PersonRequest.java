package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.PersonGenderEnum;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PersonRequest(
        @NotBlank String name,
        @NotBlank String dsDocument,
        PersonGenderEnum tpGender,
        Boolean flActive,

        List<PersonAddressRequest> personAddresses,
        List<PersonPhoneRequest> personPhones,
        List<PersonMailRequest> personMails
) {
}