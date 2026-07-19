package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.EventMessageRequest;
import br.com.clientefacil.dto.EventMessageResponse;
import br.com.clientefacil.entity.Event;
import br.com.clientefacil.entity.EventMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMessageMapper extends CoreMapper<EventMessage, EventMessageResponse> {

    @Override
    EventMessageResponse toResponse(EventMessage event);

    @Override
    List<EventMessageResponse> toResponseList(List<EventMessage> eventList);

    Event toEntity(EventMessageRequest request);
}