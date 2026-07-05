package br.com.clientefacil.entity.enums;

public enum AccountReceivableStatusEnum {
    PENDING,        // Aguardando pagamento
    PARTIALLY_PAID, // Pagamento parcial
    PAID,           // Pago integralmente
    OVERDUE,        // Vencido
    CANCELLED       // Cancelado
}