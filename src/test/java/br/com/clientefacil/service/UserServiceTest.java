package br.com.clientefacil.service;

import br.com.clientefacil.dto.ChangePasswordRequest;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.mapper.UserMapper;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * As duas guard clauses de segurança em torno da conta do usuário: não deixar reenviar confirmação
 * pra quem já confirmou, e não deixar trocar a própria senha sem confirmar a atual.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper mapper;
    @Mock
    private UserTokenService userTokenService;
    @Mock
    private EmailService emailService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, personRepository, profileRepository, passwordEncoder,
                mapper, userTokenService, emailService, "http://localhost:3000");
    }

    @Test
    void resendConfirmationThrows_whenEmailAlreadyConfirmed() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDtEmailConfirmedAt(LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resendConfirmation(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("E-mail já confirmado");

        verify(userTokenService, never()).issue(any(), any(), any());
        verify(emailService, never()).sendTemplated(any(), any(String.class), any(), any());
    }

    @Test
    void resendConfirmationIssuesNewTokenAndSendsEmail_whenNotYetConfirmed() {
        User user = new User();
        user.setId(2L);
        user.setEmail("pendente@example.com");
        user.setDtEmailConfirmedAt(null);
        when(repository.findById(2L)).thenReturn(Optional.of(user));
        when(userTokenService.issue(any(), any(), any())).thenReturn("token-novo");

        service.resendConfirmation(2L);

        verify(emailService).sendTemplated(any(), org.mockito.ArgumentMatchers.eq("pendente@example.com"), any(), any());
    }

    @Test
    void changeMyPasswordThrows_whenCurrentPasswordIsWrong() {
        User user = new User();
        user.setId(3L);
        user.setPassword("hash-atual");
        when(repository.findById(3L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);

        assertThatThrownBy(() -> service.changeMyPassword(3L, new ChangePasswordRequest("errada", "novaSenha123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Senha atual incorreta");

        verify(repository, never()).save(any());
    }

    @Test
    void changeMyPasswordEncodesAndSaves_whenCurrentPasswordMatches() {
        User user = new User();
        user.setId(4L);
        user.setPassword("hash-atual");
        when(repository.findById(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("certa", "hash-atual")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-nova");

        service.changeMyPassword(4L, new ChangePasswordRequest("certa", "novaSenha123"));

        assertThat(user.getPassword()).isEqualTo("hash-nova");
        verify(repository).save(user);
    }
}
