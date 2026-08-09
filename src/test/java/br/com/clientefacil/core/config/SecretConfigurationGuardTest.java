package br.com.clientefacil.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * jwt.secret e mail.config.encryption-key com o valor de exemplo do application.yml só podem subir
 * fora de um perfil "de verdade" (prod/production/staging) — ver javadoc de SecretConfigurationGuard
 * pro raciocínio completo.
 */
class SecretConfigurationGuardTest {

    private static final String EXAMPLE_SECRET = "12345678901234567890123456789012";
    private static final String REAL_SECRET = "um-segredo-de-verdade-configurado-via-variavel-de-ambiente";

    @Test
    void doesNotFail_whenBothSecretsAreOverridden_evenInAProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new SecretConfigurationGuard(REAL_SECRET, REAL_SECRET, environment))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotFail_whenDefaultSecretsAreUsed_inANonProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("docker");

        assertThatCode(() -> new SecretConfigurationGuard(EXAMPLE_SECRET, EXAMPLE_SECRET, environment))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotFail_whenDefaultSecretsAreUsed_withNoActiveProfileAtAll() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new SecretConfigurationGuard(EXAMPLE_SECRET, EXAMPLE_SECRET, environment))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesToStart_whenDefaultJwtSecretIsUsed_inAProductionLikeProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new SecretConfigurationGuard(EXAMPLE_SECRET, REAL_SECRET, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void refusesToStart_whenDefaultMailEncryptionKeyIsUsed_inAProductionLikeProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        assertThatThrownBy(() -> new SecretConfigurationGuard(REAL_SECRET, EXAMPLE_SECRET, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mail.config.encryption-key");
    }
}
