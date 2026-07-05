package br.com.clientefacil.entity.enums;

public enum EventStatusEnum {
    SCHEDULED,   // Agendado
    CONFIRMED,   // Confirmado pelo cliente
    IN_PROGRESS, // Atendimento em andamento
    COMPLETED,   // Atendimento concluído
    CANCELLED,   // Cancelado
    MISSED       // Cliente não compareceu
}