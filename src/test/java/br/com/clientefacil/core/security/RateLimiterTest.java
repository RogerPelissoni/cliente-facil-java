package br.com.clientefacil.core.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O "sliding window log" em si (usado por AuthService pra limitar tentativas de login/recuperação
 * de senha por e-mail) — janela, chaves independentes, e reabertura depois que tentativas antigas
 * saem da janela.
 */
class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void allowsUpToTheLimit_thenRejects() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("key", 3, Duration.ofMinutes(1))).isTrue();
        }

        assertThat(rateLimiter.tryConsume("key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void tracksEachKeyIndependently() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryConsume("email-a", 3, Duration.ofMinutes(1));
        }
        assertThat(rateLimiter.tryConsume("email-a", 3, Duration.ofMinutes(1))).isFalse();

        // outra chave começa do zero, sem "herdar" o limite de "email-a".
        assertThat(rateLimiter.tryConsume("email-b", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void allowsAgain_onceOldAttemptsSlideOutOfTheWindow() throws InterruptedException {
        Duration shortWindow = Duration.ofMillis(50);

        assertThat(rateLimiter.tryConsume("key", 1, shortWindow)).isTrue();
        assertThat(rateLimiter.tryConsume("key", 1, shortWindow)).isFalse();

        Thread.sleep(80);

        assertThat(rateLimiter.tryConsume("key", 1, shortWindow)).isTrue();
    }
}
