package br.com.clientefacil.mapper;

import br.com.clientefacil.core.CoreMapper;
import br.com.clientefacil.dto.CompanyResponse;
import br.com.clientefacil.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompanyMapper extends CoreMapper<Company, CompanyResponse> {

    @Mapping(source = "person.name", target = "personName")
    CompanyResponse toResponse(Company company);

    @Override
    List<CompanyResponse> toResponseList(List<Company> companies);
}