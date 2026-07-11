package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.EventRequest;
import br.com.clientefacil.dto.EventResponse;
import br.com.clientefacil.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper extends CoreMapper<Event, EventResponse> {

    @Override
    EventResponse toResponse(Event event);

    @Override
    List<EventResponse> toResponseList(List<Event> eventList);

    Event toEntity(EventRequest eventRequest);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(EventRequest request, @MappingTarget Event entity);
}