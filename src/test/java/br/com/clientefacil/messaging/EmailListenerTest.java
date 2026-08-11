package br.com.clientefacil.messaging;

import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.service.MailConfigService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O sentinela SIMULATE_FAILURE_VARIABLE (painel admin/dead-letters → "Simular falha (e-mail)") tem
 * uma exigência explícita, documentada no javadoc da classe: rejeitar ANTES de resolver config ou
 * tocar em SMTP, pra não gastar uma tentativa real de envio só pra testar o pipeline de DLQ.
 * <p>
 * Config do CircuitBreaker aqui é deliberadamente mais agressiva (janela de 2, mínimo de 2
 * chamadas) que a de produção (application.yml, janela de 10) — só pra não precisar de dezenas de
 * chamadas repetidas pra exercitar OPEN/CallNotPermittedException num teste unitário; o
 * comportamento testado (abre depois de falha suficiente, rejeita sem tocar o sender, é por-config)
 * é o mesmo independente do tamanho da janela.
 */
@ExtendWith(MockitoExtension.class)
class EmailListenerTest {

    @Mock
    private MailConfigService mailConfigService;
    @Mock
    private EmailSenderService emailSenderService;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private EmailListener listener;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(5))
                .recordExceptions(org.springframework.mail.MailException.class)
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
        listener = new EmailListener(mailConfigService, emailSenderService, circuitBreakerRegistry);
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
        config.setCompanyId(7L);
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

    @Test
    void receiveOpensCircuit_afterEnoughSmtpFailures_andThenRejectsWithoutCallingSender() throws Exception {
        EmailMessageDTO message = new EmailMessageDTO(9L, List.of("alguem@x.com"), "assunto", "test-email", Map.of());
        MailConfig config = new MailConfig();
        config.setCompanyId(9L);
        when(mailConfigService.resolveEffectiveConfig(9L)).thenReturn(config);
        doThrow(new MailSendException("conexão recusada")).when(emailSenderService)
                .send(any(), any(), any(), any(), any());

        // 2 falhas reais = o mínimo configurado no setUp -> circuito abre.
        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(MailSendException.class);
        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(MailSendException.class);
        assertThat(circuitBreakerRegistry.circuitBreaker("email-smtp-9").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // Terceira tentativa: circuito já aberto, nem chega a chamar o sender de novo.
        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(CallNotPermittedException.class);
        verify(emailSenderService, org.mockito.Mockito.times(2)).send(any(), any(), any(), any(), any());
    }

    @Test
    void receiveKeepsCircuitsIndependent_perCompany() throws Exception {
        MailConfig companyA = new MailConfig();
        companyA.setCompanyId(1L);
        MailConfig companyB = new MailConfig();
        companyB.setCompanyId(2L);
        when(mailConfigService.resolveEffectiveConfig(1L)).thenReturn(companyA);
        when(mailConfigService.resolveEffectiveConfig(2L)).thenReturn(companyB);

        EmailMessageDTO messageA = new EmailMessageDTO(1L, List.of("a@x.com"), "assunto", "test-email", Map.of());
        EmailMessageDTO messageB = new EmailMessageDTO(2L, List.of("b@x.com"), "assunto", "test-email", Map.of());

        doThrow(new MailSendException("fora do ar")).when(emailSenderService)
                .send(companyA, messageA.to(), messageA.subject(), messageA.template(), messageA.variables());

        assertThatThrownBy(() -> listener.receive(messageA)).isInstanceOf(MailSendException.class);
        assertThatThrownBy(() -> listener.receive(messageA)).isInstanceOf(MailSendException.class);
        assertThat(circuitBreakerRegistry.circuitBreaker("email-smtp-1").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // Empresa B nunca falhou -> continua fechada, envio normal, sem interferência da A.
        listener.receive(messageB);
        assertThat(circuitBreakerRegistry.circuitBreaker("email-smtp-2").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        verify(emailSenderService).send(companyB, messageB.to(), messageB.subject(), messageB.template(), messageB.variables());
    }

    @Test
    void receiveDoesNotCountTemplateFailure_towardTheCircuit() throws Exception {
        EmailMessageDTO message = new EmailMessageDTO(3L, List.of("a@x.com"), "assunto", "test-email", Map.of());
        MailConfig config = new MailConfig();
        config.setCompanyId(3L);
        when(mailConfigService.resolveEffectiveConfig(3L)).thenReturn(config);
        // Exceção fora da hierarquia MailException (ex: bug de template) -> não é falha de SMTP.
        doThrow(new IllegalStateException("template quebrado")).when(emailSenderService)
                .send(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> listener.receive(message)).isInstanceOf(IllegalStateException.class);

        // Mensagem continua falhando (retry+DLQ normal), mas o circuito não abre por causa disso.
        assertThat(circuitBreakerRegistry.circuitBreaker("email-smtp-3").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
