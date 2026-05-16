package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.ClientResponse;
import br.com.clientefacil.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper extends CoreMapper<Client, ClientResponse> {

    @Override
    @Mapping(source = "person.id", target = "personId")
    @Mapping(source = "person.name", target = "personName")
    ClientResponse toResponse(Client client);

    @Override
    List<ClientResponse> toResponseList(List<Client> clientList);
}