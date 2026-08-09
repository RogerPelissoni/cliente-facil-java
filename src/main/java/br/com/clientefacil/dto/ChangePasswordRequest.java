package br.com.clientefacil.dto;

import br.com.clientefacil.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

// Troca de senha self-service (diferente do update admin em UserRequest, que não pede a senha
// atual): exige currentPassword pra provar que quem está pedindo a troca é o próprio dono da conta.
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword String newPassword
) {
}
