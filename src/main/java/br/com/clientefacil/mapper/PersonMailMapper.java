package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.PersonMailRequest;
import br.com.clientefacil.dto.PersonMailResponse;
import br.com.clientefacil.entity.PersonMail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonMailMapper extends CoreMapper<PersonMail, PersonMailResponse> {

    @Override
    PersonMailResponse toResponse(PersonMail entity);

    @Override
    List<PersonMailResponse> toResponseList(List<PersonMail> entities);

    @Mapping(target = "person", ignore = true)
    PersonMail toEntity(PersonMailRequest request);
}