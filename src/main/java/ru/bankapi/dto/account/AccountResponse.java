package ru.bankapi.dto.account;

import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private CurrencyCode currency;
    private AccountStatus status;
    private LocalDateTime createdAt;

}