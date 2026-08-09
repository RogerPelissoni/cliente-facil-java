package br.com.clientefacil.core.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limitador de taxa em memória, por chave, do tipo "sliding window log": guarda o instante de cada
 * tentativa aceita dentro da janela e descarta as que já saíram dela. Simples e suficiente pra uma
 * instância só (sem Redis/cluster) — ver trade-off no javadoc de {@link #tryConsume}.
 * <p>
 * Usado hoje por {@code AuthService} pra limitar tentativas de login e de recuperação de senha por
 * e-mail (não por IP — ver comentário em AuthService.LOGIN_RATE_LIMIT sobre por quê).
 */
@Component
public class RateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    /**
     * @return {@code true} se a tentativa foi aceita (dentro do limite); {@code false} se a chave já
     * atingiu {@code maxAttempts} dentro de {@code window} — chamador deve rejeitar a requisição.
     * <p>
     * Trade-off aceito: {@code attemptsByKey} nunca remove chaves inteiras (só os timestamps
     * antigos de cada uma) — pra este uso (chave = e-mail de login/recuperação), a cardinalidade é
     * limitada pelo número de contas do sistema, não cresce sem limite. Não usar isso pra chaves
     * controladas por quem ataca (ex: um valor arbitrário do request) sem revisar esse trade-off.
     */
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        Deque<Instant> timestamps = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant now = Instant.now();

        synchronized (timestamps) {
            Instant windowStart = now.minus(window);

            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxAttempts) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}
