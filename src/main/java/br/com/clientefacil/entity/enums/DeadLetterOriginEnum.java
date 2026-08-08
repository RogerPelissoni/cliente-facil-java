package br.com.clientefacil.entity.enums;

// Qual pipeline gerou o registro em notification_dead_letter — ver
// NotificationDeadLetterListener (NOTIFICATION) e EmailDeadLetterListener (EMAIL). Tabela
// compartilhada de propósito: a auditoria é idêntica (log operacional de mensageria), então não
// há motivo pra duplicar entidade/migration/repository/mapper.
public enum DeadLetterOriginEnum {
    NOTIFICATION,
    EMAIL
}
