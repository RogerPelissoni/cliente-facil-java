package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.NotificationResponse;
import br.com.clientefacil.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper extends CoreMapper<Notification, NotificationResponse> {

    @Override
    @Mapping(target = "userId", source = "user.id")
    NotificationResponse toResponse(Notification entity);

    @Override
    List<NotificationResponse> toResponseList(List<Notification> entities);
}
