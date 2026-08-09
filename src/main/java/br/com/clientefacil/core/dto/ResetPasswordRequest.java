package br.com.clientefacil.core.dto;

import br.com.clientefacil.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @StrongPassword String newPassword
) {
}
