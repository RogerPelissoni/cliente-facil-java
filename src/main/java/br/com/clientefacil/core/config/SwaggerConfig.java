package br.com.clientefacil.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura a documentação da API via OpenAPI (Swagger).
 * <p>
 * Define o esquema de segurança baseado em JWT (Bearer Token),
 * permitindo que os endpoints protegidos sejam testados diretamente
 * pelo Swagger UI com autenticação.
 * <p>
 * Principais pontos:
 * - Registra um SecurityScheme do tipo HTTP com bearer (JWT)
 * - Aplica esse esquema globalmente na documentação (SecurityRequirement)
 * - Permite informar o token no Swagger (botão "Authorize")
 * <p>
 * Na prática, o usuário pode inserir um token JWT e realizar chamadas
 * autenticadas diretamente pela interface do Swagger.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}