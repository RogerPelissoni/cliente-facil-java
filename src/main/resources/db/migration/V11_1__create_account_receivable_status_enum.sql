CREATE TYPE account_receivable_status_enum AS ENUM (
    'PENDING',            -- Aguardando pagamento
    'PARTIALLY_PAID',     -- Pagamento parcial
    'PAID',               -- Pago integralmente
    'OVERDUE',            -- Vencido
    'CANCELLED'           -- Cancelado
);