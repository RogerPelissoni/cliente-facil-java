package br.com.clientefacil.config;

import br.com.clientefacil.repository.support.TenantAwareRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "br.com.clientefacil.repository",
        repositoryBaseClass = TenantAwareRepositoryImpl.class
)
public class JpaRepositoryConfig {
}
