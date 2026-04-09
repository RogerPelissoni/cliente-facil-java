package br.com.clientefacil.core.config;

import br.com.clientefacil.core.security.util.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configura o suporte de auditoria do Spring Data JPA.
 * <p>
 * Ao habilitar o @EnableJpaAuditing, o Spring passa a preencher automaticamente
 * campos de auditoria nas entidades (ex: createdBy, updatedBy).
 * <p>
 * O bean "authenticatedUserAuditor" define como obter o usuário atual:
 * neste caso, busca o ID do usuário autenticado via SecurityUtils.
 * <p>
 * Esse valor será utilizado automaticamente sempre que uma entidade for
 * criada ou atualizada.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "authenticatedUserAuditor")
public class JpaAuditingConfig {

    @Bean("authenticatedUserAuditor")
    public AuditorAware<Long> authenticatedUserAuditor() {
        return SecurityUtil::getAuthenticatedUserId;
    }
}