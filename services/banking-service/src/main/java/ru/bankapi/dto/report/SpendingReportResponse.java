package ru.bankapi.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Отчёт о расходах пользователя")
public class SpendingReportResponse {

    @Schema(description = "Общая сумма расходов", example = "1750.50")
    private BigDecimal totalSpent;

    @Schema(description = "Общая сумма снятий со счетов", example = "750.50")
    private BigDecimal withdrawals;

    @Schema(description = "Переводы на счета других пользователей", example = "1000.00")
    private BigDecimal outgoingTransfers;

    @Schema(description = "Дата и время формирования отчёта", example = "2026-08-31T18:00:00")
    private LocalDateTime generatedAt;
}