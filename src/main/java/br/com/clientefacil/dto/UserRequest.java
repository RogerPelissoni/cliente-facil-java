package br.com.clientefacil.dto;

import br.com.clientefacil.core.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(

        @NotBlank String name,

        @Email(message = "Email inválido")
        @NotBlank String email,

        @NotBlank
        @StrongPassword
        String password,

        @NotBlank
        String role,

        @NotNull Long personId,

        @NotNull Long profileId,

        @NotNull Long companyId
) {
}
