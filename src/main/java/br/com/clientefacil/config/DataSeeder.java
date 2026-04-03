package br.com.clientefacil.config;

import br.com.clientefacil.entity.User;
import br.com.clientefacil.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedUsers(UserRepository repository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {

            // evita duplicação
            if (repository.findByEmail("admin@admin.com").isEmpty()) {

                User admin = new User();
                admin.setName("Administrador");
                admin.setEmail("admin@admin.com");
                admin.setPassword(passwordEncoder.encode("123456"));

                repository.save(admin);

                System.out.println("✅ Usuário admin criado");
            }
        };
    }
}