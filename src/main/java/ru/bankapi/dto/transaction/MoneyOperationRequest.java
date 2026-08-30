package ru.bankapi.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MoneyOperationRequest {

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Сумма должна содержать не более двух знаков после запятой"
    )
    private BigDecimal amount;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;
}