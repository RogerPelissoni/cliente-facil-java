package br.com.clientefacil.core.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Handler responsável por tratar exceções de acesso negado (HTTP 403).
 * <p>
 * É acionado quando o usuário está autenticado, porém não possui
 * permissão suficiente para acessar o recurso solicitado.
 * <p>
 * Retorna uma resposta padronizada em JSON, facilitando o consumo
 * pelo frontend e mantendo consistência nos erros da API.
 * <p>
 * Diferença importante:
 * - 401 (Unauthorized): usuário não autenticado
 * - 403 (Forbidden): usuário autenticado, mas sem permissão
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType("application/json");

        mapper.writeValue(response.getOutputStream(), Map.of(
                "error", "Acesso negado",
                "status", 403
        ));
    }
}