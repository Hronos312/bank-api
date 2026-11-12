package ru.bankapi.mapper;

import org.springframework.stereotype.Component;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.model.BankTransaction;

@Component
public class BankTransactionMapper {

    public TransactionResponse toResponse(BankTransaction transaction) {
        TransactionResponse response = new TransactionResponse();

        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setSourceAccountId(
                transaction.getSourceAccount() == null ? null
                        : transaction.getSourceAccount().getId()
        );
        response.setDestinationAccountId(
                transaction.getDestinationAccount() == null ? null
                        : transaction.getDestinationAccount().getId()
        );
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());

        return response;
    }
}