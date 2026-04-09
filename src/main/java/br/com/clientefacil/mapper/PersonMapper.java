package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.entity.Person;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonMapper extends CoreMapper<Person, PersonResponse> {

    @Override
    PersonResponse toResponse(Person company);

    @Override
    List<PersonResponse> toResponseList(List<Person> users);

}