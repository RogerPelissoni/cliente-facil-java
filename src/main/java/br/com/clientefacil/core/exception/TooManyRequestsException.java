package br.com.clientefacil.core.exception;

/**
 * Limite de tentativas excedido (ver {@code RateLimiter}) — mapeada pra HTTP 429 em
 * {@link GlobalExceptionHandler}, distinta das demais falhas de negócio de auth (que continuam
 * caindo no handler genérico de {@link RuntimeException}) porque aqui o cliente tem uma ação óbvia
 * e correta a tomar: esperar e tentar de novo.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
