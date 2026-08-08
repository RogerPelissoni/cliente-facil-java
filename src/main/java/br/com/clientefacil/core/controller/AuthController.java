package br.com.clientefacil.core.controller;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.AuthenticatedUserResponse;
import br.com.clientefacil.core.dto.ConfirmEmailRequest;
import br.com.clientefacil.core.dto.ForgotPasswordRequest;
import br.com.clientefacil.core.dto.ResetPasswordRequest;
import br.com.clientefacil.core.dto.WsTicketResponse;
import br.com.clientefacil.core.security.WsTicketService;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final WsTicketService wsTicketService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        return service.login(request);
    }

    // Público (ver SecurityConfig) — link clicado a partir do e-mail de confirmação
    // (UserService.sendConfirmationEmail).
    @Operation(summary = "CONFIRM_EMAIL")
    @PostMapping("/confirm-email")
    public void confirmEmail(@RequestBody @Valid ConfirmEmailRequest request) {
        service.confirmEmail(request.token());
    }

    // Público, sempre 202 independente do e-mail existir ou não (evita enumeração de usuários — ver
    // AuthService.forgotPassword).
    @Operation(summary = "FORGOT_PASSWORD")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        service.forgotPassword(request.email());
    }

    // Público — link clicado a partir do e-mail de recuperação de senha.
    @Operation(summary = "RESET_PASSWORD")
    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        service.resetPassword(request);
    }

    // "Whoami": permite ao front-end descobrir o id, email e as permissões (authorities) do
    // usuário autenticado sem precisar ler o JWT (que fica em cookie httpOnly, inacessível ao
    // JS do navegador). As authorities permitem ao front esconder/desabilitar ações que o
    // usuário não teria permissão de executar de qualquer forma (o backend continua sendo a
    // fonte de verdade via @PreAuthorize — isso aqui é só para a UI não oferecer o que vai falhar).
    @GetMapping("/me")
    public AuthenticatedUserResponse me() {
        return SecurityUtil.getAuthenticatedUser()
                .map(user -> new AuthenticatedUserResponse(
                        user.getUserId(),
                        user.getUsername(),
                        user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
                ))
                .orElseThrow(() -> new AccessDeniedException("Não autenticado"));
    }

    // Emite um ticket efêmero (30s, uso único) para autenticar o handshake STOMP, no lugar de
    // expor o JWT completo ao JS do navegador — ver WsTicketService e StompAuthChannelInterceptor.
    @GetMapping("/ws-ticket")
    public WsTicketResponse wsTicket() {
        Long userId = SecurityUtil.getAuthenticatedUserId()
                .orElseThrow(() -> new AccessDeniedException("Não autenticado"));

        return new WsTicketResponse(wsTicketService.issue(userId));
    }
}