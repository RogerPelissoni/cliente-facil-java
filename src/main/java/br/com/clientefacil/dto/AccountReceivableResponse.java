package br.com.clientefacil.dto;

import java.time.LocalDate;

public record AccountReceivableResponse(
        Long id,
        Double vlTotal,
        LocalDate daDue
) {
}
