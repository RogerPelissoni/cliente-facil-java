package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.PersonRequest;
import br.com.clientefacil.dto.PersonResponse;
import br.com.clientefacil.dto.PersonWithRelationsResponse;
import br.com.clientefacil.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                PersonAddressMapper.class,
                PersonPhoneMapper.class,
                PersonMailMapper.class,
        }
)
public interface PersonMapper extends CoreMapper<Person, PersonResponse> {

    @Override
    PersonResponse toResponse(Person person);

    @Override
    List<PersonResponse> toResponseList(List<Person> personList);

    Person toEntity(PersonRequest personRequest);

    PersonWithRelationsResponse toResponseWithRelations(Person person);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "personAddresses", ignore = true)
    @Mapping(target = "personPhones", ignore = true)
    @Mapping(target = "personMails", ignore = true)
    void updateEntityFromRequest(PersonRequest request, @MappingTarget Person entity);
}