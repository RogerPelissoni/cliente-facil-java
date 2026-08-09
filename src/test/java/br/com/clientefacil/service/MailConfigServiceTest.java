package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import br.com.clientefacil.dto.MailConfigDraftTestRequest;
import br.com.clientefacil.dto.MailConfigTestRequest;
import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.entity.enums.MailEncryptionType;
import br.com.clientefacil.messaging.EmailSenderService;
import br.com.clientefacil.repository.MailConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * `resolveEffectiveConfig` (fallback empresa → base, usado pelo EmailListener em toda mensagem da
 * fila) e a resolução de senha do "Testar Conexão" (`testDraft`) — a mesma área onde já apareceu um
 * bug real nesta sessão (draft sem usuário exigindo senha à toa, corrigido na Parte 9). Justamente
 * por já ter mostrado ser fácil de errar, vale ter teste automatizado em vez de só validação manual.
 */
@ExtendWith(MockitoExtension.class)
class MailConfigServiceTest {

    @Mock
    private MailConfigRepository repository;
    @Mock
    private EmailSenderService emailSenderService;

    private MailConfigService service;

    @BeforeEach
    void setUp() {
        service = new MailConfigService(repository, emailSenderService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- resolveEffectiveConfig -------------------------------------------------------------

    @Test
    void resolveEffectiveConfig_usesCompanyConfig_whenActiveAndPresent() {
        MailConfig companyConfig = mailConfig(1L, true);
        when(repository.findByCompanyId(1L)).thenReturn(Optional.of(companyConfig));

        MailConfig result = service.resolveEffectiveConfig(1L);

        assertThat(result).isSameAs(companyConfig);
    }

    @Test
    void resolveEffectiveConfig_fallsBackToBase_whenCompanyConfigIsInactive() {
        MailConfig companyConfig = mailConfig(1L, false);
        MailConfig baseConfig = mailConfig(null, true);
        when(repository.findByCompanyId(1L)).thenReturn(Optional.of(companyConfig));
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.of(baseConfig));

        MailConfig result = service.resolveEffectiveConfig(1L);

        assertThat(result).isSameAs(baseConfig);
    }

    @Test
    void resolveEffectiveConfig_fallsBackToBase_whenCompanyHasNoConfig() {
        MailConfig baseConfig = mailConfig(null, true);
        when(repository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.of(baseConfig));

        MailConfig result = service.resolveEffectiveConfig(1L);

        assertThat(result).isSameAs(baseConfig);
    }

    @Test
    void resolveEffectiveConfig_usesBaseDirectly_whenCompanyIdIsNull() {
        MailConfig baseConfig = mailConfig(null, true);
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.of(baseConfig));

        MailConfig result = service.resolveEffectiveConfig(null);

        assertThat(result).isSameAs(baseConfig);
        verify(repository, never()).findByCompanyId(any());
    }

    @Test
    void resolveEffectiveConfig_throws_whenNothingActiveIsFound() {
        when(repository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveEffectiveConfig(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- testDraft: resolução de senha ------------------------------------------------------

    @Test
    void testDraft_usesPasswordFromRequest_whenProvided() throws Exception {
        MailConfigDraftTestRequest request = draftRequest(MailConfigTestRequest.MailConfigScope.BASE, "senha-do-form");

        service.testDraft(request);

        assertThat(captureConfigSentToEmail().getDsPassword()).isEqualTo("senha-do-form");
        verifyNoInteractions(repository);
    }

    @Test
    void testDraft_usesNullPassword_whenBlankAndNoUsername() throws Exception {
        // Servidor sem autenticação (ex: MailHog) — não faz sentido exigir senha salva se nem
        // usuário tem. Bug real que já apareceu aqui: exigir senha mesmo sem usuário no draft.
        MailConfigDraftTestRequest request = draftRequest(MailConfigTestRequest.MailConfigScope.BASE, "");

        service.testDraft(request);

        assertThat(captureConfigSentToEmail().getDsPassword()).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void testDraft_usesPersistedPassword_whenBlankButUsernamePresentAndConfigExists() throws Exception {
        MailConfig persisted = mailConfig(null, true);
        persisted.setDsPassword("senha-ja-salva");
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.of(persisted));

        MailConfigDraftTestRequest request = draftRequestWithUsername(MailConfigTestRequest.MailConfigScope.BASE, "");

        service.testDraft(request);

        assertThat(captureConfigSentToEmail().getDsPassword()).isEqualTo("senha-ja-salva");
    }

    @Test
    void testDraft_throws_whenBlankPasswordAndUsernamePresentButNothingPersisted() {
        when(repository.findByCompanyIdIsNull()).thenReturn(Optional.empty());

        MailConfigDraftTestRequest request = draftRequestWithUsername(MailConfigTestRequest.MailConfigScope.BASE, "");

        assertThatThrownBy(() -> service.testDraft(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Informe a senha");
    }

    @Test
    void testDraft_resolvesPersistedPassword_forCompanyScope() throws Exception {
        MailConfig persisted = mailConfig(42L, true);
        persisted.setDsPassword("senha-da-empresa");
        when(repository.findByCompanyId(42L)).thenReturn(Optional.of(persisted));

        authenticateAsCompany(42L);
        MailConfigDraftTestRequest request = draftRequestWithUsername(MailConfigTestRequest.MailConfigScope.COMPANY, "");
        service.testDraft(request);

        assertThat(captureConfigSentToEmail().getDsPassword()).isEqualTo("senha-da-empresa");
    }

    private MailConfig captureConfigSentToEmail() throws Exception {
        ArgumentCaptor<MailConfig> captor = ArgumentCaptor.forClass(MailConfig.class);
        verify(emailSenderService).send(captor.capture(), anyList(), any(), any());
        return captor.getValue();
    }

    private void authenticateAsCompany(Long companyId) {
        var authenticatedUser = new AuthenticatedUser(1L, companyId, "user@example.com", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of()));
    }

    private MailConfigDraftTestRequest draftRequest(MailConfigTestRequest.MailConfigScope scope, String password) {
        return new MailConfigDraftTestRequest(scope, "destino@x.com", "mailhog", 1025, "",
                password, MailEncryptionType.NONE, "Teste", "no-reply@clientefacil.local");
    }

    private MailConfigDraftTestRequest draftRequestWithUsername(MailConfigTestRequest.MailConfigScope scope, String password) {
        return new MailConfigDraftTestRequest(scope, "destino@x.com", "smtp.real.com", 587, "usuario-real",
                password, MailEncryptionType.TLS, "Teste", "no-reply@clientefacil.local");
    }

    private MailConfig mailConfig(Long companyId, boolean active) {
        MailConfig config = new MailConfig();
        config.setCompanyId(companyId);
        config.setFlActive(active);
        config.setDsHost("host");
        config.setNrPort(587);
        config.setTpEncryption(MailEncryptionType.TLS);
        config.setDsFromName("Cliente Fácil");
        config.setDsFromAddress("no-reply@clientefacil.local");
        return config;
    }
}
