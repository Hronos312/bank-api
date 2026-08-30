package ru.bankapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.service.TransactionService;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/{accountId}/deposit")
    public TransactionResponse deposit(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        return transactionService.deposit(accountId, userDetails.getUsername(), request);
    }

    @PostMapping("/{accountId}/withdraw")
    public TransactionResponse withdraw(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        return transactionService.withdraw(accountId, userDetails.getUsername(), request);
    }
}