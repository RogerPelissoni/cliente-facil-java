package br.com.clientefacil.dto;

// Resumo para o painel administrativo (Parte 7 do docs/mensageria-e-websocket.md). Pensado para
// crescer: cada novo "insight" (ex: falhas por motivo, série temporal) vira um campo novo aqui
// sem quebrar o que já existe.
public record NotificationDeadLetterStatsResponse(
        long totalPending,
        long totalResolved,
        long totalLast24h
) {
}
