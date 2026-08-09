package br.com.clientefacil.core.service;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.ResetPasswordRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Os três "portões" de login (usuário existe, senha bate, e-mail confirmado) e a delegação de
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

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repository, passwordEncoder, jwtService, userTokenService, emailService, FRONTEND_URL);
    }

    @Test
    void loginThrows_whenUserDoesNotExist() {
        when(repository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AuthRequest("ninguem@x.com", "123456")))
                .hasMessage("Usuário não encontrado");
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
