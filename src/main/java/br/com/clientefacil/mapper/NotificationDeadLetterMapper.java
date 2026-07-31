package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.NotificationDeadLetterResponse;
import br.com.clientefacil.entity.NotificationDeadLetter;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationDeadLetterMapper extends CoreMapper<NotificationDeadLetter, NotificationDeadLetterResponse> {
}
