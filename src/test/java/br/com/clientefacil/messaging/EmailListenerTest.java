package br.com.clientefacil.messaging;

import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.service.MailConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O sentinela SIMULATE_FAILURE_VARIABLE (painel admin/dead-letters → "Simular falha (e-mail)") tem
 * uma exigência explícita, documentada no javadoc da classe: rejeitar ANTES de resolver config ou
 * tocar em SMTP, pra não gastar uma tentativa real de envio só pra testar o pipeline de DLQ.
 */
@ExtendWith(MockitoExtension.class)
class EmailListenerTest {

    @Mock
    private MailConfigService mailConfigService;
    @Mock
    private EmailSenderService emailSenderService;

    private EmailListener listener;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        listener = new EmailListener(mailConfigService, emailSenderService);
    }

    @Test
    void receiveThrowsImmediately_whenSimulateFailureFlagSet_withoutResolvingConfigOrSending() {
        EmailMessageDTO message = new EmailMessageDTO(null, List.of("alguem@x.com"), "assunto", "test-email",
                Map.of(EmailListener.SIMULATE_FAILURE_VARIABLE, true));

        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(mailConfigService, emailSenderService);
    }

    @Test
    void receiveSendsNormally_whenSimulateFailureFlagAbsent() throws Exception {
        EmailMessageDTO message = new EmailMessageDTO(7L, List.of("alguem@x.com"), "assunto", "test-email",
                Map.of("sentAt", "08/08/2026 10:00:00"));
        MailConfig config = new MailConfig();
        when(mailConfigService.resolveEffectiveConfig(7L)).thenReturn(config);

        listener.receive(message);

        verify(emailSenderService).send(config, message.to(), message.subject(), message.template(), message.variables());
    }

    @Test
    void receiveSendsNormally_whenVariablesDoNotContainTheFlagAtAll() throws Exception {
        EmailMessageDTO message = new EmailMessageDTO(null, List.of("alguem@x.com"), "assunto", "dead-letter-alert",
                Map.of("origin", "NOTIFICATION"));
        MailConfig config = new MailConfig();
        when(mailConfigService.resolveEffectiveConfig(null)).thenReturn(config);

        listener.receive(message);

        verify(emailSenderService).send(any(), any(), any(), any(), any());
    }
}
