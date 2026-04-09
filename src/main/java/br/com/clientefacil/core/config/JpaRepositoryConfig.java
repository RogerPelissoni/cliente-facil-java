package br.com.clientefacil.core.config;

import br.com.clientefacil.core.repository.TenantScopedRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configura os repositórios JPA da aplicação.
 * <p>
 * Define o pacote base onde o Spring deve buscar os repositories e,
 * principalmente, sobrescreve a implementação padrão do Spring Data JPA
 * para utilizar o TenantAwareRepositoryImpl como base.
 * <p>
 * Isso permite centralizar comportamentos comuns a todos os repositórios,
 * como, por exemplo:
 * - aplicação automática de filtros por tenant (company_id)
 * - regras de isolamento de dados entre empresas
 * - customizações globais de acesso ao banco
 * <p>
 * Na prática, todos os repositories da aplicação passam a herdar essa
 * implementação customizada ao invés dá padrão do Spring.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "br.com.clientefacil.repository",
        repositoryBaseClass = TenantScopedRepositoryImpl.class
)
public class JpaRepositoryConfig {
}