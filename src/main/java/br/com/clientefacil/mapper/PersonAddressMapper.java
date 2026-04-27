package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.PersonAddressRequest;
import br.com.clientefacil.dto.PersonAddressResponse;
import br.com.clientefacil.entity.PersonAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonAddressMapper extends CoreMapper<PersonAddress, PersonAddressResponse> {

    @Override
    PersonAddressResponse toResponse(PersonAddress entity);

    @Override
    List<PersonAddressResponse> toResponseList(List<PersonAddress> entities);

    @Mapping(target = "person", ignore = true)
    PersonAddress toEntity(PersonAddressRequest request);
}