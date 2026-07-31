package br.com.clientefacil.messaging;

import br.com.clientefacil.entity.enums.NotificationTypeEnum;

// Corpo que trafega pela fila do RabbitMQ (publisher escreve, listener lê e persiste).
public record NotificationMessageDTO(
        Long userId,
        NotificationTypeEnum type,
        String title,
        String message
) {
}
