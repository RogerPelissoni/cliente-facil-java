package br.com.clientefacil.core.controller;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.AuthenticatedUserResponse;
import br.com.clientefacil.core.security.util.SecurityUtil;
import br.com.clientefacil.core.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        return service.login(request);
    }

    // "Whoami": permite ao front-end descobrir o id do usuário autenticado sem precisar
    // ler o JWT (que fica em cookie httpOnly, inacessível ao JS do navegador). Usado para
    // abrir a conexão WebSocket de notificações já sabendo para qual usuário rotear.
    @GetMapping("/me")
    public AuthenticatedUserResponse me() {
        return SecurityUtil.getAuthenticatedUser()
                .map(user -> new AuthenticatedUserResponse(user.getUserId(), user.getUsername()))
                .orElseThrow(() -> new AccessDeniedException("Não autenticado"));
    }
}