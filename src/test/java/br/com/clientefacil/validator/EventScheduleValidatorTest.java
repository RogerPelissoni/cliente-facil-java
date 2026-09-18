package br.com.clientefacil.validator;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.exception.BusinessException;
import br.com.clientefacil.repository.EventServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventScheduleValidatorTest {

    @Mock
    private EventServiceRepository eventServiceRepository;

    private EventScheduleValidator validator;

    private static final Long PROFESSIONAL_ID = 1L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 1, 10, 10, 0);

    @BeforeEach
    void setUp() {
        validator = new EventScheduleValidator(eventServiceRepository);
    }

    @Test
    void deveLancarBusinessExceptionQuandoHouverSobreposicaoDeHorario() {
        when(eventServiceRepository.existsOverlapping(PROFESSIONAL_ID, START, END, null, EventStatusEnum.CANCELLED))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validateNoConflict(PROFESSIONAL_ID, START, END, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O profissional já possui outro atendimento agendado nesse horário.");
    }

    @Test
    void naoDeveLancarExcecaoQuandoNaoHouverSobreposicao() {
        when(eventServiceRepository.existsOverlapping(PROFESSIONAL_ID, START, END, null, EventStatusEnum.CANCELLED))
                .thenReturn(false);

        assertThatCode(() -> validator.validateNoConflict(PROFESSIONAL_ID, START, END, null))
                .doesNotThrowAnyException();
    }

    @Test
    void deveExcluirOProprioEventoDaChecagemAoAtualizar() {
        Long eventId = 42L;
        when(eventServiceRepository.existsOverlapping(PROFESSIONAL_ID, START, END, eventId, EventStatusEnum.CANCELLED))
                .thenReturn(false);

        validator.validateNoConflict(PROFESSIONAL_ID, START, END, eventId);

        verify(eventServiceRepository).existsOverlapping(PROFESSIONAL_ID, START, END, eventId, EventStatusEnum.CANCELLED);
    }
}
