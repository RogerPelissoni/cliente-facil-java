package br.com.clientefacil.validator;

import br.com.clientefacil.exception.BusinessException;
import br.com.clientefacil.repository.AccountReceivableMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountReceivableValidator {

    private final AccountReceivableMovementRepository movementRepository;

    public void canUpdate(Long accountReceivableId) {
        validateUnlocked(accountReceivableId, "editar");
    }

    public void canDelete(Long accountReceivableId) {
        validateUnlocked(accountReceivableId, "remover");
    }

    private void validateUnlocked(Long accountReceivableId, String operation) {
        if (movementRepository.existsByAccountReceivableId(accountReceivableId)) {
            throw new BusinessException(
                    "Operação bloqueada, não é possível " + operation + " um título que possui movimentações financeiras."
            );
        }
    }
}
