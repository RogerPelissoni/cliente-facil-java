package br.com.clientefacil.validator;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.exception.BusinessException;
import br.com.clientefacil.repository.EventServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventScheduleValidator {

    private final EventServiceRepository eventServiceRepository;

    // excludeEventId nulo no create (nenhum evento a excluir); no update, é o próprio evento sendo
    // editado — sem isso ele sempre entraria em conflito com o próprio horário.
    public void validateNoConflict(
            Long professionalId,
            LocalDateTime dtStart,
            LocalDateTime dtEnd,
            Long excludeEventId
    ) {
        boolean hasConflict = eventServiceRepository.existsOverlapping(
                professionalId,
                dtStart,
                dtEnd,
                excludeEventId,
                EventStatusEnum.CANCELLED
        );

        if (hasConflict) {
            throw new BusinessException("O profissional já possui outro atendimento agendado nesse horário.");
        }
    }
}
