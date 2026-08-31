package ru.bankapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.User;
import ru.bankapi.service.ReportService;
import ru.bankapi.service.TransactionService;
import ru.bankapi.service.TransferService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
class SpendingReportIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("bank_api_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configurePostgres(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private ReportService reportService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void spendingReportShouldExcludeTransfersBetweenOwnAccounts() {
        User firstUser = createUser(
                "ivan@example.com",
                "+79990000001"
        );

        User secondUser = createUser(
                "recipient@example.com",
                "+79990000002"
        );

        BankAccount firstAccount = createAccount(
                firstUser,
                "40817810000000000001",
                "1000.00"
        );

        BankAccount secondOwnAccount = createAccount(
                firstUser,
                "40817810000000000002",
                "0.00"
        );

        BankAccount foreignAccount = createAccount(
                secondUser,
                "40817810000000000003",
                "0.00"
        );

        MoneyOperationRequest withdrawRequest =
                new MoneyOperationRequest();

        withdrawRequest.setAmount(
                new BigDecimal("100.00")
        );

        transactionService.withdraw(
                firstAccount.getId(),
                firstUser.getEmail(),
                withdrawRequest
        );

        TransferRequest ownTransfer =
                new TransferRequest();

        ownTransfer.setDestinationAccountNumber(
                secondOwnAccount.getAccountNumber()
        );

        ownTransfer.setAmount(
                new BigDecimal("200.00")
        );

        transferService.transfer(
                firstAccount.getId(),
                firstUser.getEmail(),
                ownTransfer
        );

        TransferRequest foreignTransfer =
                new TransferRequest();

        foreignTransfer.setDestinationAccountNumber(
                foreignAccount.getAccountNumber()
        );

        foreignTransfer.setAmount(
                new BigDecimal("300.00")
        );

        transferService.transfer(
                firstAccount.getId(),
                firstUser.getEmail(),
                foreignTransfer
        );

        SpendingReportResponse report =
                reportService.getSpendingReport(
                        firstUser.getEmail()
                );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                report.getWithdrawals()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(
                                report.getOutgoingTransfers()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(
                                report.getTotalSpent()
                        )
        );
    }

    private User createUser(
            String email,
            String phone
    ) {
        User user = new User();

        user.setEmail(email);
        user.setPasswordHash("test-password-hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setBirthDate(
                LocalDate.of(2000, 1, 1)
        );
        user.setPhone(phone);
        user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    private BankAccount createAccount(
            User user,
            String accountNumber,
            String balance
    ) {
        BankAccount account = new BankAccount();

        account.setAccountNumber(accountNumber);
        account.setUser(user);
        account.setBalance(
                new BigDecimal(balance)
        );
        account.setStatus(AccountStatus.ACTIVE);

        return bankAccountRepository.save(account);
    }
}