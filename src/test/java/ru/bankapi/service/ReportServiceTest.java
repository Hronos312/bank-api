package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.model.User;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSpendingReportShouldReturnAggregatedValues() {
        String email = "ivan@example.com";

        User user = createUser(
                1L,
                email
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(transactionRepository.sumBySourceUserIdAndType(
                1L,
                TransactionType.WITHDRAWAL
        )).thenReturn(new BigDecimal("750.50"));

        when(transactionRepository.sumOutgoingTransfersToOtherUsers(
                1L,
                TransactionType.TRANSFER
        )).thenReturn(new BigDecimal("1000.00"));

        SpendingReportResponse result =
                reportService.getSpendingReport(email);

        assertNotNull(result);

        assertEquals(
                0,
                new BigDecimal("750.50")
                        .compareTo(result.getWithdrawals())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.getOutgoingTransfers())
        );

        assertEquals(
                0,
                new BigDecimal("1750.50")
                        .compareTo(result.getTotalSpent())
        );

        assertNotNull(result.getGeneratedAt());

        verify(transactionRepository)
                .sumBySourceUserIdAndType(
                        1L,
                        TransactionType.WITHDRAWAL
                );

        verify(transactionRepository)
                .sumOutgoingTransfersToOtherUsers(
                        1L,
                        TransactionType.TRANSFER
                );
    }

    @Test
    void getSpendingReportShouldReturnZeroWhenUserHasNoExpenses() {
        String email = "ivan@example.com";

        User user = createUser(
                1L,
                email
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(transactionRepository.sumBySourceUserIdAndType(
                1L,
                TransactionType.WITHDRAWAL
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.sumOutgoingTransfersToOtherUsers(
                1L,
                TransactionType.TRANSFER
        )).thenReturn(BigDecimal.ZERO);

        SpendingReportResponse result =
                reportService.getSpendingReport(email);

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getWithdrawals()
                )
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getOutgoingTransfers()
                )
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getTotalSpent()
                )
        );
    }

    @Test
    void getSpendingReportShouldThrowWhenUserDoesNotExist() {
        String email = "missing@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> reportService.getSpendingReport(email)
        );
    }

    private User createUser(
            Long id,
            String email
    ) {
        User user = new User();

        user.setId(id);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }
}