package ru.bankapi.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Данные денежной операции")
public class MoneyOperationRequest {

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    @Digits(integer = 17, fraction = 2, message = "Сумма должна содержать не более двух знаков после запятой")
    @Schema(description = "Сумма операции", example = "500.00")
    private BigDecimal amount;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    @Schema(description = "Необязательное описание операции", example = "Пополнение счёта")
    private String description;
}