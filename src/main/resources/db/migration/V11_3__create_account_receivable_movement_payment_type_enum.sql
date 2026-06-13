CREATE TYPE account_receivable_movement_payment_type_enum AS ENUM (
    'CASH',            -- Dinheiro
    'PIX',             -- PIX
    'DEBIT_CARD',      -- Cartão de débito
    'CREDIT_CARD',     -- Cartão de crédito
    'BANK_TRANSFER',   -- TED/DOC/Transferência
    'CHECK',           -- Cheque
    'BOLETO',          -- Boleto
    'OTHER'            -- Outros
);