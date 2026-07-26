package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.EventRequest;
import br.com.clientefacil.dto.EventResponse;
import br.com.clientefacil.dto.EventWithRelationsResponse;
import br.com.clientefacil.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                EventServiceMapper.class
        }
)
public interface EventMapper extends CoreMapper<Event, EventResponse> {

    @Override
    EventResponse toResponse(Event event);

    @Mapping(target = "accountReceivable", source = "eventService.accountReceivable")
    EventWithRelationsResponse toResponseComplete(Event event);

    @Override
    List<EventResponse> toResponseList(List<Event> eventList);

    @Mapping(target = "eventService", ignore = true)
    Event toEntity(EventRequest eventRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventService", ignore = true)
    void updateEntityFromRequest(EventRequest request, @MappingTarget Event entity);
}