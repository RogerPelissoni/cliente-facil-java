package br.com.clientefacil.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AccountReceivableRequest(
        @NotNull Double vlTotal,
        @NotNull LocalDate daDue
) {
}