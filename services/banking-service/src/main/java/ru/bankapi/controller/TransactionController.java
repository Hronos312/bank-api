package ru.bankapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.service.MoneyIdempotencyService;
import ru.bankapi.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Money operations", description = "Пополнение, снятие и история операций")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final MoneyIdempotencyService idempotency;

    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Пополнить счёт", description = "Пополняет активный банковский счёт текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт успешно пополнен"),
            @ApiResponse(responseCode = "400", description = "Некорректная сумма операции"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Операция недоступна для пользователя или счёта")
    })
    public TransactionResponse deposit(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(name = MoneyIdempotencyService.HEADER, required = false) String key,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        String subject = userDetails.getUsername();
        if (key == null) {
            return transactionService.deposit(accountId, subject, request);
        }
        return idempotency.execute(Long.parseLong(subject), key, new MoneyIdempotencyService.Operation(
                TransactionType.DEPOSIT, accountId, request.getAmount(), request.getDescription(), null),
                () -> transactionService.deposit(accountId, subject, request));
    }

    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Снять деньги", description = "Списывает деньги с активного банковского счёта")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Средства успешно списаны"),
            @ApiResponse(responseCode = "400", description = "Некорректная сумма операции"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Недостаточно средств или операция недоступна")
    })
    public TransactionResponse withdraw(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(name = MoneyIdempotencyService.HEADER, required = false) String key,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        String subject = userDetails.getUsername();
        if (key == null) {
            return transactionService.withdraw(accountId, subject, request);
        }
        return idempotency.execute(Long.parseLong(subject), key, new MoneyIdempotencyService.Operation(
                TransactionType.WITHDRAWAL, accountId, request.getAmount(), request.getDescription(), null),
                () -> transactionService.withdraw(accountId, subject, request));
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Получить историю операций", description = "Возвращает операции по счёту от новых к старым")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "История операций получена"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден")
    })
    public List<TransactionResponse> getTransactionHistory(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return transactionService.getTransactionHistory(accountId, userDetails.getUsername());
    }
}
