package ru.bankapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Ошибка API")
public class ErrorResponse {

    @Schema(description = "Дата и время возникновения ошибки", example = "2026-08-31T18:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP-код ответа", example = "404")
    private int status;

    @Schema(description = "Внутренний код ошибки", example = "NOT_FOUND")
    private String code;

    @Schema(description = "Описание ошибки", example = "Счёт не найден")
    private String message;

    @Schema(description = "Endpoint, на котором возникла ошибка", example = "/api/accounts/99")
    private String path;
}