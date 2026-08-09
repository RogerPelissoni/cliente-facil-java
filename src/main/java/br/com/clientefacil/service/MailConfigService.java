package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.dto.MailConfigDraftTestRequest;
import br.com.clientefacil.dto.MailConfigRequest;
import br.com.clientefacil.dto.MailConfigResponse;
import br.com.clientefacil.dto.MailConfigTestRequest;
import br.com.clientefacil.entity.MailConfig;
import br.com.clientefacil.messaging.EmailSenderService;
import br.com.clientefacil.messaging.template.TestEmailTemplate;
import br.com.clientefacil.repository.MailConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MailConfigService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final MailConfigRepository repository;
    private final EmailSenderService emailSenderService;

    /**
     * Resolve a config SMTP que o EmailListener deve efetivamente usar para uma mensagem: a da
     * empresa, se existir e estiver ativa; senão a config base do sistema. Chamado a partir de um
     * contexto sem usuário autenticado (o RabbitMQ listener roda em background), então o
     * tenantFilter do Hibernate não está habilitado e a busca por companyId enxerga qualquer
     * empresa — é por isso que essa resolução não pode ficar a cargo do filtro automático.
     */
    public MailConfig resolveEffectiveConfig(Long companyId) {
        if (companyId != null) {
            var companyConfig = repository.findByCompanyId(companyId).filter(MailConfig::isFlActive);
            if (companyConfig.isPresent()) {
                return companyConfig.get();
            }
        }

        return repository.findByCompanyIdIsNull()
                .filter(MailConfig::isFlActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma configuração de e-mail ativa encontrada (nem da empresa, nem base)"));
    }

    public MailConfigResponse getBase() {
        return toResponse(findBaseEntity());
    }

    public MailConfigResponse getMine() {
        Long companyId = currentCompanyId();
        return repository.findByCompanyId(companyId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Sua empresa ainda não tem uma configuração de e-mail própria"));
    }

    public MailConfigResponse upsertBase(MailConfigRequest request) {
        MailConfig entity = repository.findByCompanyIdIsNull().orElseGet(() -> {
            MailConfig created = new MailConfig();
            created.markAsGlobalScope();
            return created;
        });

        fillEntityByRequest(entity, request);
        return toResponse(repository.save(entity));
    }

    public MailConfigResponse upsertMine(MailConfigRequest request) {
        Long companyId = currentCompanyId();
        MailConfig entity = repository.findByCompanyId(companyId).orElseGet(() -> {
            MailConfig created = new MailConfig();
            created.setCompanyId(companyId);
            return created;
        });

        fillEntityByRequest(entity, request);
        return toResponse(repository.save(entity));
    }

    public void deleteBase() {
        repository.delete(findBaseEntity());
    }

    public void deleteMine() {
        Long companyId = currentCompanyId();
        MailConfig entity = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Sua empresa ainda não tem uma configuração de e-mail própria"));
        repository.delete(entity);
    }

    // Testa os dados ATUAIS do formulário (possivelmente não salvos ainda), sem persistir nada e sem
    // passar pela fila — envio síncrono, pra dar feedback de sucesso/erro na hora, na própria resposta
    // HTTP (diferente de sendTest, que enfileira e sempre responde 202).
    public void testDraft(MailConfigDraftTestRequest request) {
        MailConfig transientConfig = buildTransientConfig(request);
        var template = new TestEmailTemplate(LocalDateTime.now().format(DATE_FORMAT));

        try {
            emailSenderService.send(transientConfig, List.of(request.to()), "Cliente Fácil — e-mail de teste", template);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao testar conexão SMTP: " + e.getMessage(), e);
        }
    }

    private MailConfig buildTransientConfig(MailConfigDraftTestRequest request) {
        MailConfig config = new MailConfig();
        config.setDsHost(request.dsHost());
        config.setNrPort(request.nrPort());
        config.setDsUsername(request.dsUsername());
        config.setDsPassword(StringUtils.hasText(request.dsPassword())
                ? request.dsPassword()
                : resolvePersistedPassword(request));
        config.setTpEncryption(request.tpEncryption());
        config.setDsFromName(request.dsFromName());
        config.setDsFromAddress(request.dsFromAddress());
        return config;
    }

    // dsPassword em branco no draft = "use a senha já salva pro scope" (mesma semântica do save, ver
    // fillEntityByRequest) — permite testar depois de mexer só em outro campo (porta, from, etc) sem
    // precisar redigitar a senha, já que ela nunca volta pro front (WRITE_ONLY). Servidor sem usuário
    // (ex: MailHog) não exige senha nenhuma — mesma regra de "auth só se tiver username" que
    // EmailSenderService já aplica ao montar o JavaMailSenderImpl — então só vamos atrás de uma senha
    // salva quando o draft realmente tem um usuário preenchido.
    private String resolvePersistedPassword(MailConfigDraftTestRequest request) {
        if (!StringUtils.hasText(request.dsUsername())) {
            return null;
        }

        var persisted = request.scope() == MailConfigTestRequest.MailConfigScope.COMPANY
                ? repository.findByCompanyId(currentCompanyId())
                : repository.findByCompanyIdIsNull();

        String password = persisted.map(MailConfig::getDsPassword).orElse(null);

        if (!StringUtils.hasText(password)) {
            throw new ResourceNotFoundException(
                    "Informe a senha para testar uma configuração nova (nenhuma senha salva encontrada)");
        }

        return password;
    }

    private MailConfig findBaseEntity() {
        return repository.findByCompanyIdIsNull()
                .orElseThrow(() -> new ResourceNotFoundException("A configuração base de e-mail ainda não foi definida"));
    }

    private Long currentCompanyId() {
        return SecurityUtil.getAuthenticatedCompanyId()
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não está vinculado a uma empresa"));
    }

    private void fillEntityByRequest(MailConfig entity, MailConfigRequest request) {
        entity.setDsHost(request.dsHost());
        entity.setNrPort(request.nrPort());
        entity.setDsUsername(request.dsUsername());
        // Senha em branco numa atualização = mantém a já salva (ver MailConfigRequest).
        if (StringUtils.hasText(request.dsPassword())) {
            entity.setDsPassword(request.dsPassword());
        }
        entity.setTpEncryption(request.tpEncryption());
        entity.setDsFromName(request.dsFromName());
        entity.setDsFromAddress(request.dsFromAddress());
        entity.setFlActive(request.flActive());
    }

    private MailConfigResponse toResponse(MailConfig entity) {
        return new MailConfigResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getDsHost(),
                entity.getNrPort(),
                entity.getDsUsername(),
                StringUtils.hasText(entity.getDsPassword()),
                entity.getTpEncryption(),
                entity.getDsFromName(),
                entity.getDsFromAddress(),
                entity.isFlActive()
        );
    }
}
