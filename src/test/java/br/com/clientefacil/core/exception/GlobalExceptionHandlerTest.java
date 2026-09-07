package br.com.clientefacil.core.exception;

import br.com.clientefacil.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException/ResourceNotFoundException caíam no catch-all de RuntimeException (500) antes
 * disso existir — a mensagem já saía certa pro usuário (o front lê data.message independente do
 * status), mas o código HTTP não. Fica mais visível agora que o ForeignKeyDeletionGuard (soft delete,
 * core/hibernate/) passa a lançar BusinessException com bem mais frequência que antes.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessReturnsConflictWithMessage() {
        ResponseEntity<?> response = handler.handleBusiness(new BusinessException("título possui movimentações"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of(
                "status", 409,
                "error", "Operação bloqueada",
                "message", "título possui movimentações"
        ));
    }

    @Test
    void handleNotFoundReturnsNotFoundWithMessage() {
        ResponseEntity<?> response = handler.handleNotFound(new ResourceNotFoundException("Pessoa não encontrada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of(
                "status", 404,
                "error", "Não encontrado",
                "message", "Pessoa não encontrada"
        ));
    }

    // ForeignKeyDeletionGuard lança de dentro de um listener do Hibernate que só roda no flush/commit
    // da transação — nesse ponto o Spring já embrulhou a exceção original numa
    // TransactionSystemException, ver comentário em handleTransactionSystem.
    @Test
    void handleTransactionSystemUnwrapsBusinessExceptionCause() {
        var ex = new TransactionSystemException("Could not commit JPA transaction",
                new BusinessException("existe um registro vinculado"));

        ResponseEntity<?> response = handler.handleTransactionSystem(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of(
                "status", 409,
                "error", "Operação bloqueada",
                "message", "existe um registro vinculado"
        ));
    }

    @Test
    void handleTransactionSystemUnwrapsResourceNotFoundExceptionCause() {
        var ex = new TransactionSystemException("Could not commit JPA transaction",
                new ResourceNotFoundException("Pessoa não encontrada"));

        ResponseEntity<?> response = handler.handleTransactionSystem(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleTransactionSystemFallsBackTo500_whenCauseIsUnrelated() {
        var ex = new TransactionSystemException("Could not commit JPA transaction",
                new IllegalStateException("outra coisa qualquer"));

        ResponseEntity<?> response = handler.handleTransactionSystem(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
