package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NotificationSendRequest(
        @NotEmpty List<@NotNull Long> userIds,
        @NotBlank String title,
        @NotBlank String message,
        NotificationTypeEnum type
) {
}
