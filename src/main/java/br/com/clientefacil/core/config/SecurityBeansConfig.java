package br.com.clientefacil.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Define beans relacionados à segurança da aplicação.
 * <p>
 * Neste caso, registra um PasswordEncoder baseado em BCrypt,
 * utilizado para criptografar senhas antes de persistir no banco
 * e para validação durante o processo de autenticação.
 * <p>
 * O BCrypt é um algoritmo seguro, com salt embutido e resistente
 * a ataques de força bruta, sendo o padrão recomendado pelo Spring Security.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}