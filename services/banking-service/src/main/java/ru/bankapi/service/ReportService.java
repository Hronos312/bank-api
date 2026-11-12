package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.identity.IdentityDirectory;
import ru.bankapi.identity.IdentityDirectory.UserIdentity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BankTransactionRepository transactionRepository;
    private final IdentityDirectory identityDirectory;

    @Transactional(readOnly = true)
    public SpendingReportResponse getSpendingReport(String email) {
        UserIdentity user = identityDirectory.findByUsername(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        BigDecimal withdrawals = transactionRepository.sumBySourceUserIdAndType(user.id(), TransactionType.WITHDRAWAL);

        BigDecimal outgoingTransfers = transactionRepository.sumOutgoingTransfersToOtherUsers(
                user.id(),
                TransactionType.TRANSFER
        );

        BigDecimal totalSpent = withdrawals.add(outgoingTransfers);

        SpendingReportResponse response = new SpendingReportResponse();

        response.setWithdrawals(withdrawals);
        response.setOutgoingTransfers(outgoingTransfers);
        response.setTotalSpent(totalSpent);
        response.setGeneratedAt(LocalDateTime.now());

        return response;
    }
}
