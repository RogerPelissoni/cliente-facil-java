CREATE TYPE event_status_enum AS ENUM (
    'SCHEDULED',    -- Agendado
    'CONFIRMED',    -- Confirmado pelo cliente
    'IN_PROGRESS',  -- Atendimento em andamento
    'COMPLETED',    -- Atendimento concluído
    'CANCELLED',    -- Cancelado
    'MISSED'        -- Cliente não compareceu
);