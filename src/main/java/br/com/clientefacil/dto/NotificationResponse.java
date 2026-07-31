package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.NotificationStatusEnum;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationTypeEnum tpType,
        NotificationStatusEnum tpStatus,
        String dsTitle,
        String dsMessage,
        LocalDateTime dtRead,
        LocalDateTime createdAt
) {
}
