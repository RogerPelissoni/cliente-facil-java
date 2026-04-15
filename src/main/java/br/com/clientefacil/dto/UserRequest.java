package br.com.clientefacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank String name,

        @Email(message = "Email inválido")
        @NotBlank String email,

        @NotBlank
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password,

        @NotNull Long personId,

        @NotNull Long profileId,

        @NotNull Long companyId
) {
}