package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BankTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SpendingReportResponse getSpendingReport(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        BigDecimal withdrawals = transactionRepository.sumBySourceUserIdAndType(user.getId(), TransactionType.WITHDRAWAL);

        BigDecimal outgoingTransfers = transactionRepository.sumOutgoingTransfersToOtherUsers(
                user.getId(),
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