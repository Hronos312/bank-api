package ru.bankapi.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Банковская операция")
public class TransactionResponse {

    @Schema(description = "Идентификатор транзакции", example = "15")
    private Long id;

    @Schema(description = "Тип операции", example = "TRANSFER")
    private TransactionType type;

    @Schema(description = "Идентификатор счёта-источника. Может отсутствовать для пополнения", example = "1")
    private Long sourceAccountId;

    @Schema(description = "Идентификатор счёта-получателя. Может отсутствовать для снятия", example = "2")
    private Long destinationAccountId;

    @Schema(description = "Сумма операции", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Описание операции", example = "Перевод другому пользователю")
    private String description;

    @Schema(description = "Дата и время выполнения операции", example = "2026-08-31T18:00:00")
    private LocalDateTime createdAt;
}