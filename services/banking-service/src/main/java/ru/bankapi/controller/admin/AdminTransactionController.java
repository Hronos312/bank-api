package ru.bankapi.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.transaction.TransactionPageResponse;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.service.admin.AdminTransactionService;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@Tag(name = "Admin - Transactions", description = "Просмотр журнала банковских операций")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    @GetMapping
    @Operation(summary = "Получить журнал операций", description = "Возвращает транзакции постранично от новых к старым")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница транзакций получена"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public TransactionPageResponse getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminTransactionService.getTransactions(page, size);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Получить транзакцию", description = "Возвращает банковскую операцию по её id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Транзакция найдена"),
            @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
    })
    public TransactionResponse getTransaction(@PathVariable Long transactionId) {
        return adminTransactionService.getTransaction(transactionId);
    }
}