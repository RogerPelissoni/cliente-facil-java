package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.PersonPhoneRequest;
import br.com.clientefacil.dto.PersonPhoneResponse;
import br.com.clientefacil.entity.PersonPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonPhoneMapper extends CoreMapper<PersonPhone, PersonPhoneResponse> {

    @Override
    PersonPhoneResponse toResponse(PersonPhone entity);

    @Override
    List<PersonPhoneResponse> toResponseList(List<PersonPhone> entities);

    @Mapping(target = "person", ignore = true)
    PersonPhone toEntity(PersonPhoneRequest request);
}