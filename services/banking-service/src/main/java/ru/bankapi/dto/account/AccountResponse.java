package ru.bankapi.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Банковский счёт")
public class AccountResponse {

    @Schema(description = "Идентификатор счёта", example = "1")
    private Long id;

    @Schema(description = "20-значный номер банковского счёта", example = "40817810537009986573")
    private String accountNumber;

    @Schema(description = "Текущий баланс счёта", example = "749.50")
    private BigDecimal balance;

    @Schema(description = "Валюта счёта", example = "RUB")
    private CurrencyCode currency;

    @Schema(description = "Статус счёта", example = "ACTIVE")
    private AccountStatus status;

    @Schema(description = "Дата и время открытия счёта", example = "2026-08-31T18:00:00")
    private LocalDateTime createdAt;
}