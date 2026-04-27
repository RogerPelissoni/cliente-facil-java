package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.PersonGenderEnum;

import java.util.List;

public record PersonWithRelationsResponse(
        Long id,
        String name,
        String dsDocument,
        PersonGenderEnum tpGender,
        Boolean flActive,

        List<PersonAddressResponse> personAddresses,
        List<PersonPhoneResponse> personPhones,
        List<PersonMailResponse> personMails
) {
}
