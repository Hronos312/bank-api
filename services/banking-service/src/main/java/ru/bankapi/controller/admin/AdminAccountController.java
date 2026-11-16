package ru.bankapi.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.service.admin.AdminAccountService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@Tag(name = "Admin - Accounts", description = "Администрирование банковских счетов")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    @Operation(summary = "Получить все счета",  description = "Возвращает счета всех пользователей системы")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список счетов получен"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public List<AccountResponse> getAccounts() {
        return adminAccountService.getAccounts();
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Получить счёт по id", description = "Возвращает счёт по его id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт найден"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден")
    })
    public AccountResponse getAccount(@PathVariable Long accountId) {
        return adminAccountService.getAccount(accountId);
    }

    @PatchMapping("/{accountId}/block")
    @Operation(summary = "Заблокировать счёт", description = "Переводит активный счёт в статус BLOCKED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт заблокирован"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Изменение статуса невозможно")
    })
    public AccountResponse blockAccount(@PathVariable Long accountId) {
        return adminAccountService.blockAccount(accountId);
    }

    @PatchMapping("/{accountId}/unblock")
    @Operation(summary = "Разблокировать счёт", description = "Переводит заблокированный счёт в статус ACTIVE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт разблокирован"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Изменение статуса невозможно")
    })
    public AccountResponse unblockAccount(@PathVariable Long accountId) {
        return adminAccountService.unblockAccount(accountId);
    }

    @PatchMapping("/{accountId}/close")
    @Operation(summary = "Закрыть счёт", description = "Закрывает счёт с нулевым балансом. Связанная карта также переводится в CLOSED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт закрыт"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Счёт уже закрыт или имеет ненулевой баланс")
    })
    public AccountResponse closeAccount(@PathVariable Long accountId) {
        return adminAccountService.closeAccount(accountId);
    }
}