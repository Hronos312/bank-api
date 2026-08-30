package ru.bankapi.controller;

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
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/{sourceAccountId}/transfer")
    public TransactionResponse transfer(
            @PathVariable Long sourceAccountId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request
    ) {
        return transferService.transfer(sourceAccountId, userDetails.getUsername(), request);
    }

}
