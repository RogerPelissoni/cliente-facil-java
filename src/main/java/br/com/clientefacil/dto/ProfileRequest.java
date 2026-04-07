package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(
        @NotBlank String name
) {
}