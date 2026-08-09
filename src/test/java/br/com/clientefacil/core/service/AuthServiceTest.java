package br.com.clientefacil.core.service;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.ResetPasswordRequest;
import br.com.clientefacil.core.exception.TooManyRequestsException;
import br.com.clientefacil.core.security.RateLimiter;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import br.com.clientefacil.messaging.template.EmailTemplate;
import br.com.clientefacil.messaging.template.PasswordResetTemplate;
import br.com.clientefacil.repository.UserRepository;
import br.com.clientefacil.service.EmailService;
import br.com.clientefacil.service.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Os "portões" de login (rate limit, usuário existe, conta bloqueada, senha bate, e-mail
 * confirmado), o bloqueio de conta após tentativas seguidas de senha errada, e a delegação de
 * forgot/reset/confirm pro UserTokenService — a lógica de autenticação em si, sem precisar subir
 * Spring/banco pra validar cada branch.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String FRONTEND_URL = "http://localhost:3000";

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserTokenService userTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private RateLimiter rateLimiter;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repository, passwordEncoder, jwtService, userTokenService, emailService, rateLimiter, FRONTEND_URL);
        // Rate limit "passa" por padrão em todo teste — só os testes de rate limit em si
        // sobrescrevem isso pra `false`. Sem isso, todo teste existente cairia no 429 primeiro.
        lenient().when(rateLimiter.tryConsume(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void loginThrows_withGenericMessage_whenUserDoesNotExist() {
        // Mensagem igual à de senha errada de propósito (ver teste abaixo) — mensagens diferentes
        // pra "não existe" e "senha errada" permitem descobrir quais e-mails têm conta no sistema.
        when(repository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AuthRequest("ninguem@x.com", "123456")))
                .hasMessage("Credenciais inválidas");
    }

    @Test
    void loginThrows_whenRateLimitExceeded_beforeEvenLookingUpTheUser() {
        when(rateLimiter.tryConsume(eq("login:ninguem@x.com"), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthRequest("ninguem@x.com", "123456")))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void loginThrows_whenPasswordIsWrong() {
        User user = confirmedUser();
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthRequest(user.getEmail(), "errada")))
                .hasMessage("Credenciais inválidas");
    }

    @Test
    void loginRegistersFailedAttempt_withoutLockingYet_whenBelowTheThreshold() {
        User user = confirmedUser();
        user.setNrFailedLoginAttempts(2);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthRequest(user.getEmail(), "errada")))
                .hasMessage("Credenciais inválidas");

        assertThat(user.getNrFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.getDtLockedUntil()).isNull();
        verify(repository).save(user);
    }

    @Test
    void loginLocksTheAccount_onTheNthConsecutiveFailedAttempt() {
        User user = confirmedUser();
        user.setNrFailedLoginAttempts(4); // a próxima falha é a 5ª — atinge o limite.
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthRequest(user.getEmail(), "errada")));

        // contador zera junto do bloqueio — a próxima sequência de tentativas, depois de expirar,
        // começa do zero, não já "quase bloqueada" de novo.
        assertThat(user.getNrFailedLoginAttempts()).isZero();
        assertThat(user.getDtLockedUntil()).isAfter(LocalDateTime.now());
        verify(repository).save(user);
    }

    @Test
    void loginThrows_whenAccountIsLocked_withoutEvenCheckingThePassword() {
        User user = confirmedUser();
        user.setDtLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new AuthRequest(user.getEmail(), "123456")))
                .hasMessageContaining("bloqueada");

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginAllowsAgain_onceTheLockoutHasExpired() {
        User user = confirmedUser();
        user.setDtLockedUntil(LocalDateTime.now().minusMinutes(1)); // expirou há 1 minuto
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getEmail())).thenReturn("jwt-de-teste");

        AuthResponse response = service.login(new AuthRequest(user.getEmail(), "123456"));

        assertThat(response.token()).isEqualTo("jwt-de-teste");
    }

    @Test
    void loginResetsFailedAttemptCounter_onSuccessfulLogin() {
        User user = confirmedUser();
        user.setNrFailedLoginAttempts(3);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getEmail())).thenReturn("jwt-de-teste");

        service.login(new AuthRequest(user.getEmail(), "123456"));

        assertThat(user.getNrFailedLoginAttempts()).isZero();
        verify(repository).save(user);
    }

    @Test
    void loginThrows_whenEmailNotConfirmed() {
        User user = confirmedUser();
        user.setDtEmailConfirmedAt(null);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> service.login(new AuthRequest(user.getEmail(), "123456")))
                .hasMessageContaining("não confirmado");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginSucceeds_whenPasswordCorrectAndEmailConfirmed() {
        User user = confirmedUser();
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getEmail())).thenReturn("jwt-de-teste");

        AuthResponse response = service.login(new AuthRequest(user.getEmail(), "123456"));

        assertThat(response.token()).isEqualTo("jwt-de-teste");
    }

    @Test
    void forgotPasswordThrows_whenRateLimitExceeded_beforeEvenLookingUpTheUser() {
        when(rateLimiter.tryConsume(eq("forgot-password:ninguem@x.com"), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.forgotPassword("ninguem@x.com"))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(repository, userTokenService, emailService);
    }

    @Test
    void forgotPasswordIssuesTokenAndSendsEmailWithResetLink_whenUserExists() {
        User user = confirmedUser();
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userTokenService.issue(eq(user), eq(UserTokenTypeEnum.PASSWORD_RESET), any()))
                .thenReturn("raw-token-123");

        service.forgotPassword(user.getEmail());

        ArgumentCaptor<EmailTemplate> captor = ArgumentCaptor.forClass(EmailTemplate.class);
        verify(emailService).sendTemplated(any(), eq(user.getEmail()), anyString(), captor.capture());

        assertThat(captor.getValue()).isInstanceOf(PasswordResetTemplate.class);
        var template = (PasswordResetTemplate) captor.getValue();
        assertThat(template.resetUrl()).isEqualTo(FRONTEND_URL + "/auth/reset-password?token=raw-token-123");
    }

    @Test
    void forgotPasswordDoesNothing_whenEmailDoesNotExist() {
        // Sempre "sucesso" do ponto de vista do chamador (o controller sempre responde 202) — mas
        // internamente não pode emitir token nem mandar e-mail nenhum, senão vaza quais e-mails têm
        // conta (enumeração de usuários). Ver docs/product/1_business-rules.md.
        when(repository.findByEmail("naoexiste@x.com")).thenReturn(Optional.empty());

        service.forgotPassword("naoexiste@x.com");

        verifyNoInteractions(userTokenService, emailService);
    }

    @Test
    void resetPasswordConsumesTokenAndEncodesNewPassword() {
        User user = confirmedUser();
        when(userTokenService.consume("raw-token", UserTokenTypeEnum.PASSWORD_RESET)).thenReturn(user);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-nova-senha");

        service.resetPassword(new ResetPasswordRequest("raw-token", "novaSenha123"));

        assertThat(user.getPassword()).isEqualTo("hash-nova-senha");
        verify(repository).save(user);
    }

    @Test
    void confirmEmailConsumesTokenAndStampsConfirmationTimestamp() {
        User user = confirmedUser();
        user.setDtEmailConfirmedAt(null);
        when(userTokenService.consume("raw-token", UserTokenTypeEnum.EMAIL_CONFIRMATION)).thenReturn(user);

        service.confirmEmail("raw-token");

        assertThat(user.getDtEmailConfirmedAt()).isNotNull();
        verify(repository).save(user);
    }

    private User confirmedUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("hash-atual");
        user.setDtEmailConfirmedAt(LocalDateTime.now());
        return user;
    }
}
