package ru.bankapi.dto.transfer;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Номер счёта получателя обязателен")
    @Size(min = 20, max = 20, message = "Номер счёта должен содержать 20 символов")
    private String destinationAccountNumber;

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
