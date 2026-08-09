package br.com.clientefacil.service;

import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.UserToken;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import br.com.clientefacil.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mecânica de token de uso único (confirmação de e-mail + recuperação de senha, ver
 * docs/guides/2_authentication.md) — a peça mais sensível a nível de segurança que este projeto tem
 * até aqui, por isso o candidato mais claro a teste unitário: uma falha aqui não quebra visivelmente
 * nada (o link simplesmente "funciona errado"), então precisa de garantia automatizada.
 */
@ExtendWith(MockitoExtension.class)
class UserTokenServiceTest {

    @Mock
    private UserTokenRepository repository;

    private UserTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserTokenService(repository);
        user = new User();
        user.setEmail("user@example.com");
    }

    @Test
    void issuePersistsOnlyTheHash_neverTheRawToken() {
        String rawToken = service.issue(user, UserTokenTypeEnum.PASSWORD_RESET, Duration.ofHours(1));

        ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
        verify(repository).save(captor.capture());
        UserToken saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTpType()).isEqualTo(UserTokenTypeEnum.PASSWORD_RESET);
        assertThat(saved.getDtUsedAt()).isNull();
        assertThat(saved.getDtExpiresAt()).isCloseTo(LocalDateTime.now().plusHours(1), within(5, ChronoUnit.SECONDS));

        // O hash nunca pode ser igual ao token cru (senão não seria hash de nada), e precisa ter
        // cara de SHA-256 em hexadecimal (64 caracteres, só [0-9a-f]).
        assertThat(saved.getDsTokenHash())
                .isNotEqualTo(rawToken)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    void issueInvalidatesAnyExistingUnusedTokenOfTheSameTypeForTheSameUser() {
        UserToken oldToken = new UserToken();
        oldToken.setUser(user);
        oldToken.setTpType(UserTokenTypeEnum.PASSWORD_RESET);
        when(repository.findAllByUserAndTpTypeAndDtUsedAtIsNull(user, UserTokenTypeEnum.PASSWORD_RESET))
                .thenReturn(List.of(oldToken));

        service.issue(user, UserTokenTypeEnum.PASSWORD_RESET, Duration.ofHours(1));

        // Mesmo mecanismo de "esse token não serve mais" que consume() já respeita (filtra por
        // dtUsedAt IS NULL) — não precisa de um estado novo só pra isso.
        assertThat(oldToken.getDtUsedAt()).isNotNull();
        verify(repository).saveAll(List.of(oldToken));
    }

    @Test
    void issueDoesNotTouchTokensOfADifferentType_orWithNoneToInvalidate() {
        when(repository.findAllByUserAndTpTypeAndDtUsedAtIsNull(user, UserTokenTypeEnum.EMAIL_CONFIRMATION))
                .thenReturn(List.of());

        service.issue(user, UserTokenTypeEnum.EMAIL_CONFIRMATION, Duration.ofDays(7));

        // Nada pra invalidar — nem chama saveAll (evita um round-trip ao banco à toa, caso comum:
        // primeiro token emitido pra esse usuário/tipo).
        verify(repository, never()).saveAll(any());
    }

    @Test
    void issueGeneratesATokenDiferenteACadaChamada() {
        String first = service.issue(user, UserTokenTypeEnum.EMAIL_CONFIRMATION, Duration.ofDays(7));
        String second = service.issue(user, UserTokenTypeEnum.EMAIL_CONFIRMATION, Duration.ofDays(7));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void consumeReturnsUserAndMarksTokenUsed_whenTokenIsValid() {
        UserToken persisted = issueAndCaptureSaved(UserTokenTypeEnum.EMAIL_CONFIRMATION, Duration.ofDays(1));
        String rawToken = lastIssuedRawToken;

        when(repository.findByDsTokenHashAndDtUsedAtIsNull(persisted.getDsTokenHash()))
                .thenReturn(Optional.of(persisted));

        User result = service.consume(rawToken, UserTokenTypeEnum.EMAIL_CONFIRMATION);

        assertThat(result).isSameAs(user);
        assertThat(persisted.getDtUsedAt()).isNotNull();
    }

    @Test
    void consumeThrows_whenTokenDoesNotExist() {
        when(repository.findByDsTokenHashAndDtUsedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("token-qualquer", UserTokenTypeEnum.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Link inválido ou expirado");
    }

    @Test
    void consumeThrows_whenTypeDoesNotMatch() {
        // Emitido como EMAIL_CONFIRMATION, mas alguém tenta consumir como PASSWORD_RESET (ex: link
        // de confirmação usado na tela de reset de senha) — precisa falhar, mesmo o hash existindo.
        UserToken persisted = issueAndCaptureSaved(UserTokenTypeEnum.EMAIL_CONFIRMATION, Duration.ofDays(1));
        String rawToken = lastIssuedRawToken;

        when(repository.findByDsTokenHashAndDtUsedAtIsNull(persisted.getDsTokenHash()))
                .thenReturn(Optional.of(persisted));

        assertThatThrownBy(() -> service.consume(rawToken, UserTokenTypeEnum.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Link inválido ou expirado");
    }

    @Test
    void consumeThrows_whenTokenAlreadyExpired() {
        // TTL negativo = já nasce expirado, sem precisar de Thread.sleep pra simular passagem de tempo.
        UserToken persisted = issueAndCaptureSaved(UserTokenTypeEnum.PASSWORD_RESET, Duration.ofSeconds(-1));
        String rawToken = lastIssuedRawToken;

        when(repository.findByDsTokenHashAndDtUsedAtIsNull(persisted.getDsTokenHash()))
                .thenReturn(Optional.of(persisted));

        assertThatThrownBy(() -> service.consume(rawToken, UserTokenTypeEnum.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Link inválido ou expirado");
    }

    @Test
    void consumeThrows_whenTokenAlreadyUsed() {
        // Simula o comportamento real da query (WHERE dt_used_at IS NULL): depois de usado, o
        // repository simplesmente não acha mais o token.
        UserToken persisted = issueAndCaptureSaved(UserTokenTypeEnum.PASSWORD_RESET, Duration.ofHours(1));
        String rawToken = lastIssuedRawToken;

        when(repository.findByDsTokenHashAndDtUsedAtIsNull(persisted.getDsTokenHash()))
                .thenReturn(Optional.of(persisted))
                .thenReturn(Optional.empty());

        service.consume(rawToken, UserTokenTypeEnum.PASSWORD_RESET);

        assertThatThrownBy(() -> service.consume(rawToken, UserTokenTypeEnum.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Link inválido ou expirado");
    }

    private String lastIssuedRawToken;

    private UserToken issueAndCaptureSaved(UserTokenTypeEnum type, Duration ttl) {
        lastIssuedRawToken = service.issue(user, type, ttl);

        ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
