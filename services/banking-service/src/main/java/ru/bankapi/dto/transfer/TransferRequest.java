package ru.bankapi.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Данные банковского перевода")
public class TransferRequest {

    @NotBlank(message = "Номер счёта получателя обязателен")
    @Size(min = 20, max = 20, message = "Номер счёта должен содержать 20 символов")
    @Schema(description = "Номер счёта получателя", example = "40817810537009986574")
    private String destinationAccountNumber;

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    @Digits(integer = 17, fraction = 2, message = "Сумма должна содержать не более двух знаков после запятой")
    @Schema(description = "Сумма перевода", example = "1000.00")
    private BigDecimal amount;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    @Schema(description = "Необязательное описание перевода", example = "Возврат долга")
    private String description;
}