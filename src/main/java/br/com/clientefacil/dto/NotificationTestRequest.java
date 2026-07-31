package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import jakarta.validation.constraints.NotBlank;

public record NotificationTestRequest(
        @NotBlank String content,
        NotificationTypeEnum type
) {
}
