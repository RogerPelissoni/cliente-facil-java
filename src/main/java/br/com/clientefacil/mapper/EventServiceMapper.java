package br.com.clientefacil.mapper;

import br.com.clientefacil.dto.EventServiceResponse;
import br.com.clientefacil.entity.EventService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventServiceMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "professionalId", source = "professional.id")
    @Mapping(target = "accountReceivableId", source = "accountReceivable.id")
    EventServiceResponse toResponse(EventService entity);
}