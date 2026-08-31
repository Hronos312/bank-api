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
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.service.TransferService;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Переводы между банковскими счетами")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/{sourceAccountId}/transfer")
    @Operation(summary = "Перевести деньги", description = "Переводит средства с собственного счёта на счёт по номеру")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Перевод выполнен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные перевода"),
            @ApiResponse(responseCode = "404", description = "Исходный или счёт получателя не найден"),
            @ApiResponse(responseCode = "409", description = "Недостаточно средств или перевод недоступен")
    })
    public TransactionResponse transfer(
            @PathVariable Long sourceAccountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request
    ) {
        return transferService.transfer(sourceAccountId, userDetails.getUsername(), request);
    }

}
