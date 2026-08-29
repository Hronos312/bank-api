package ru.bankapi.mapper;

import org.springframework.stereotype.Component;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.model.BankAccount;

@Component
public class BankAccountMapper {

    public AccountResponse toResponse(BankAccount account) {
        AccountResponse response = new AccountResponse();

        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setBalance(account.getBalance());
        response.setCurrency(account.getCurrency());
        response.setStatus(account.getStatus());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }

}