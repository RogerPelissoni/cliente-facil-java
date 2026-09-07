package br.com.clientefacil.core.exception;

import br.com.clientefacil.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "errors", errors
        ));
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Violacao de integridade de dados",
                "message", "Verifique os dados enviados (campos obrigatorios ou relacionamentos)"
        ));
    }

    // BusinessException/ResourceNotFoundException caíam no catch-all RuntimeException abaixo (500)
    // até aqui — a mensagem amigável já saía certa (o front lê data.message independente do
    // status), mas o código HTTP não. Fica mais visível com os validadores de soft delete
    // (validator/*DeletionValidator.java), que passam a lançar BusinessException com bem mais
    // frequência que antes.
    @SuppressWarnings("unused")
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Operação bloqueada",
                "message", ex.getMessage()
        ));
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", "Não encontrado",
                "message", ex.getMessage()
        ));
    }

    // ForeignKeyDeletionGuard (core/hibernate/) lança BusinessException/ResourceNotFoundException de
    // dentro de um PreDeleteEventListener — que roda no flush do Hibernate, geralmente só disparado
    // no commit da transação (fim do método @Transactional do repository.delete(...)), não na hora
    // da chamada em si. Nesse ponto o Spring já embrulhou a exceção original numa
    // TransactionSystemException ("Could not commit JPA transaction") — sem isto aqui, ela cai no
    // catch-all de RuntimeException (500) em vez do 409/404 que a exceção original pediria.
    // getMostSpecificCause() desembrulha a cadeia toda (não só um nível).
    @SuppressWarnings("unused")
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<?> handleTransactionSystem(TransactionSystemException ex) {
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof BusinessException businessException) {
            return handleBusiness(businessException);
        }
        if (cause instanceof ResourceNotFoundException notFoundException) {
            return handleNotFound(notFoundException);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Erro interno",
                "message", ex.getMessage()
        ));
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "error", "Acesso negado",
                "message", ex.getMessage()
        ));
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "status", 429,
                "error", "Muitas tentativas",
                "message", ex.getMessage()
        ));
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Erro interno",
                "message", ex.getMessage()
        ));
    }
}
