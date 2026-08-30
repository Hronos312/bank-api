package ru.bankapi.dto.transaction;

import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

}