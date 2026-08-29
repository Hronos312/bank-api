package ru.bankapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.service.BankAccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = bankAccountService.createAccount(userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AccountResponse> getAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        return bankAccountService.getAccounts(userDetails.getUsername());
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable Long accountId, @AuthenticationPrincipal UserDetails userDetails) {
        return bankAccountService.getAccount(accountId, userDetails.getUsername());
    }
}