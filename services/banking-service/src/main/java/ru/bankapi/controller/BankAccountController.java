package ru.bankapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts", description = "Банковские счета пользователя")
@SecurityRequirement(name = "bearerAuth")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    @Operation(summary = "Открыть банковский счёт", description = "Создаёт новый рублёвый счёт текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Счёт успешно создан"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "409", description = "Операция недоступна в текущем состоянии")
    })
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = bankAccountService.createAccount(userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить счета", description = "Возвращает все банковские счета текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Список счетов получен")
    public List<AccountResponse> getAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        return bankAccountService.getAccounts(userDetails.getUsername());
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Получить счёт", description = "Возвращает счёт текущего пользователя по его id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт найден"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден")
    })
    public AccountResponse getAccount(@PathVariable Long accountId, @AuthenticationPrincipal UserDetails userDetails) {
        return bankAccountService.getAccount(accountId, userDetails.getUsername());
    }
}