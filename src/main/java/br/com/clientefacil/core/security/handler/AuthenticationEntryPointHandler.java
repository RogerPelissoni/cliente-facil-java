package br.com.clientefacil.core.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

/**
 * Handler responsável por tratar falhas de autenticação (HTTP 401).
 * <p>
 * É acionado quando uma requisição tenta acessar um recurso protegido
 * sem estar autenticada (ex: token ausente, inválido ou expirado).
 * <p>
 * Retorna uma resposta padronizada em JSON, contendo detalhes do erro,
 * facilitando o consumo pelo frontend e mantendo consistência na API.
 * <p>
 * Diferença importante:
 * - 401 (Unauthorized): usuário não autenticado
 * - 403 (Forbidden): usuário autenticado, mas sem permissão
 */
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json");

        mapper.writeValue(response.getOutputStream(), Map.of(
                "error", "Usuário não autenticado",
                "message", authException.getMessage(),
                "path", request.getRequestURI(),
                "status", 401
        ));
    }
}