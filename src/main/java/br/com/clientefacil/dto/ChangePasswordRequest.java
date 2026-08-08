package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Troca de senha self-service (diferente do update admin em UserRequest, que não pede a senha
// atual): exige currentPassword pra provar que quem está pedindo a troca é o próprio dono da conta.
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6) String newPassword
) {
}
