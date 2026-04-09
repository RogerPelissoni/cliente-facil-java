package br.com.clientefacil.core.config;

import br.com.clientefacil.core.security.JwtFilter;
import br.com.clientefacil.core.security.handler.AuthenticationEntryPointHandler;
import br.com.clientefacil.core.security.handler.CustomAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configura a segurança da aplicação com Spring Security.
 * <p>
 * Define as regras de autenticação/autorização, tratamento de exceções
 * e integra o fluxo de autenticação via JWT.
 * <p>
 * Principais responsabilidades:
 * - Desabilitar CSRF (adequado para APIs stateless com JWT)
 * - Definir rotas públicas (login, swagger, error)
 * - Exigir autenticação para demais endpoints
 * - Configurar tratamento de exceções de segurança:
 * - AuthenticationEntryPoint: quando o usuário não está autenticado (401)
 * - AccessDeniedHandler: quando o usuário não tem permissão (403)
 * - Adicionar o JwtFilter antes do filtro padrão de autenticação,
 * permitindo validar o token e popular o contexto de segurança
 * <p>
 * O @EnableMethodSecurity permite uso de anotações como @PreAuthorize
 * nos métodos (ex: controle fino de permissões).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new AuthenticationEntryPointHandler())
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}