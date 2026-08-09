package br.com.clientefacil.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * jwt.secret e mail.config.encryption-key são segredos versionados no application.yml — qualquer
 * pessoa com acesso ao repositório os conhece. application.yml já permite sobrescrever os dois via
 * env (JWT_SECRET / MAIL_CONFIG_ENCRYPTION_KEY), mas nada impedia esquecer de configurar a env e
 * subir mesmo assim usando o valor de exemplo — silenciosamente exposto.
 * <p>
 * Esse componente falha o boot da aplicação (não só loga um aviso) se algum dos dois ainda estiver
 * no valor de exemplo E o perfil ativo parecer produção. Em qualquer outro perfil (dev, docker local,
 * test, ou nenhum perfil definido) o valor de exemplo continua permitido — não muda nada do fluxo de
 * desenvolvimento/CI atual. A validação só entra em ação no dia em que a aplicação for implantada com
 * um perfil "de verdade" (ex: SPRING_PROFILES_ACTIVE=prod) sem configurar os segredos reais.
 */
@Slf4j
@Component
public class SecretConfigurationGuard {

    private static final String EXAMPLE_SECRET_VALUE = "12345678901234567890123456789012";

    private static final Set<String> PRODUCTION_LIKE_PROFILES = Set.of("prod", "production", "staging");

    public SecretConfigurationGuard(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${mail.config.encryption-key}") String mailConfigEncryptionKey,
            Environment environment
    ) {
        List<String> exposedProperties = new ArrayList<>();

        if (EXAMPLE_SECRET_VALUE.equals(jwtSecret)) {
            exposedProperties.add("jwt.secret");
        }

        if (EXAMPLE_SECRET_VALUE.equals(mailConfigEncryptionKey)) {
            exposedProperties.add("mail.config.encryption-key");
        }

        if (exposedProperties.isEmpty()) {
            return;
        }

        if (isProductionLikeProfile(environment)) {
            throw new IllegalStateException(
                    "Recusando subir: as seguintes propriedades ainda estão com o valor de exemplo do "
                            + "application.yml: " + String.join(", ", exposedProperties) + ". O perfil ativo ("
                            + String.join(",", environment.getActiveProfiles()) + ") parece produção. Configure as "
                            + "variáveis de ambiente correspondentes (JWT_SECRET / MAIL_CONFIG_ENCRYPTION_KEY) "
                            + "antes de subir a aplicação.");
        }

        log.warn(
                "As seguintes propriedades ainda estão com o valor de exemplo do application.yml: {}. Tudo bem em "
                        + "dev/docker/test, mas configure a variável de ambiente correspondente antes de rodar em "
                        + "produção.",
                String.join(", ", exposedProperties));
    }

    private boolean isProductionLikeProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(PRODUCTION_LIKE_PROFILES::contains);
    }
}
