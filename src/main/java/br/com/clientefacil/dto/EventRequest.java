package br.com.clientefacil.dto;

import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(

        @Size(max = 100)
        @NotBlank String dsTitle,

        String dsDescription,

        @NotNull LocalDateTime dtStart,

        @NotNull LocalDateTime dtEnd,

        @NotNull EventStatusEnum tpStatus,
        
        @NotNull EventTypeEnum tpEvent
) {
}
