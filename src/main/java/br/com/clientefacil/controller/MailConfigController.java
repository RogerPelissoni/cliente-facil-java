package br.com.clientefacil.controller;

import br.com.clientefacil.dto.MailConfigDraftTestRequest;
import br.com.clientefacil.dto.MailConfigRequest;
import br.com.clientefacil.dto.MailConfigResponse;
import br.com.clientefacil.dto.MailConfigTestRequest;
import br.com.clientefacil.service.EmailService;
import br.com.clientefacil.service.MailConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD da configuração SMTP (base do sistema + da empresa autenticada) e uma rota de teste pra
 * validar o envio sem precisar de um gatilho de negócio real (mesmo espírito de
 * POST /notifications/test — ver docs/guides/1_messaging-and-websocket.md).
 * <p>
 * "Base" é deliberadamente protegida pela mesma MAIL_CONFIG_MANAGE que a config da própria
 * empresa, não por um papel "super-admin" cross-tenant (que o projeto ainda não modela): qualquer
 * empresa com essa permissão consegue alterar o envio de e-mails do sistema (ex: alerta de dead-
 * letter). Trade-off aceitável para a fase atual do projeto (ainda não está em produção),
 * documentado por completude — mesmo padrão de honestidade sobre limitações já usado no ws-ticket.
 */
@RestController
@RequestMapping("/api/v1/mail-configs")
@RequiredArgsConstructor
public class MailConfigController {

    private final MailConfigService service;
    private final EmailService emailService;

    @Operation(summary = "GET_BASE")
    @GetMapping("/base")
    @PreAuthorize("hasAuthority('MAIL_CONFIG_VIEW')")
    public MailConfigResponse getBase() {
        return service.getBase();
    }

    @Operation(summary = "UPSERT_BASE")
    @PutMapping("/base")
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public MailConfigResponse upsertBase(@RequestBody @Valid MailConfigRequest request) {
        return service.upsertBase(request);
    }

    @Operation(summary = "DELETE_BASE")
    @DeleteMapping("/base")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public void deleteBase() {
        service.deleteBase();
    }

    @Operation(summary = "GET_MINE")
    @GetMapping("/company")
    @PreAuthorize("hasAuthority('MAIL_CONFIG_VIEW')")
    public MailConfigResponse getMine() {
        return service.getMine();
    }

    @Operation(summary = "UPSERT_MINE")
    @PutMapping("/company")
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public MailConfigResponse upsertMine(@RequestBody @Valid MailConfigRequest request) {
        return service.upsertMine(request);
    }

    @Operation(summary = "DELETE_MINE")
    @DeleteMapping("/company")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public void deleteMine() {
        service.deleteMine();
    }

    @Operation(summary = "TEST")
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public void test(@RequestBody @Valid MailConfigTestRequest request) {
        // scope=COMPANY valida explicitamente que a empresa TEM uma config própria antes de
        // enfileirar — sem isso, um teste "da empresa" sem config própria cairia silenciosamente
        // na base e o admin acharia, errado, que a config da empresa está funcionando.
        Long companyId = null;
        if (request.scope() == MailConfigTestRequest.MailConfigScope.COMPANY) {
            companyId = service.getMine().companyId();
        }

        emailService.sendTest(companyId, request.to());
    }

    // Testa os dados ATUAIS do formulário (host/porta/usuário/senha/etc), sem exigir que já tenham
    // sido salvos — diferente de /test, que só valida o que já está persistido. Síncrono: responde
    // 200 no sucesso ou propaga o erro (ver MailConfigService.testDraft), pra o front dar feedback
    // imediato de "Testar Conexão".
    @Operation(summary = "TEST_DRAFT")
    @PostMapping("/test-draft")
    @PreAuthorize("hasAuthority('MAIL_CONFIG_MANAGE')")
    public void testDraft(@RequestBody @Valid MailConfigDraftTestRequest request) {
        service.testDraft(request);
    }
}
