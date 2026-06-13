package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.ProfessionalResponse;
import br.com.clientefacil.entity.Professional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfessionalMapper extends CoreMapper<Professional, ProfessionalResponse> {

    @Override
    @Mapping(source = "person.id", target = "personId")
    @Mapping(source = "person.name", target = "personName")
    ProfessionalResponse toResponse(Professional company);

    @Override
    List<ProfessionalResponse> toResponseList(List<Professional> companies);
}
