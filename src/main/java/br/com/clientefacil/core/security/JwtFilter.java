package br.com.clientefacil.core.security;

import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import br.com.clientefacil.core.service.AuthenticatedUserService;
import br.com.clientefacil.core.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro responsável por processar autenticação via JWT em cada requisição.
 * <p>
 * Executado uma única vez por requisição (OncePerRequestFilter), intercepta
 * chamadas HTTP para verificar a presença de um token Bearer no header Authorization.
 * <p>
 * Fluxo:
 * 1. Verifica se existe o header Authorization com prefixo "Bearer"
 * 2. Extrai o token JWT
 * 3. Valida o token (integridade + expiração)
 * 4. Recupera os dados do usuário (AuthenticatedUser)
 * 5. Cria um Authentication e registra no SecurityContext
 * <p>
 * Após esse processo, o usuário passa a ser considerado autenticado pelo
 * Spring Security, permitindo uso de:
 * - @PreAuthorize
 * - SecurityContextHolder
 * - regras de autorização
 * <p>
 * Caso o token seja inválido ou ocorra erro, a requisição segue sem autenticação.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        try {
            String email = jwtService.extractEmail(token);

            if (email != null
                    && jwtService.isTokenValid(token, email)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                AuthenticatedUser userDetails = authenticatedUserService.loadByEmail(email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }
}