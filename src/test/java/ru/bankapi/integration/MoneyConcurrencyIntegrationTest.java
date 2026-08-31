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
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.User;
import ru.bankapi.service.TransactionService;
import ru.bankapi.service.TransferService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
class MoneyConcurrencyIntegrationTest {

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
    void concurrentWithdrawalsShouldNotOverdrawAccount()
            throws Exception {

        User user = createUser(
                "ivan@example.com",
                "+79990000001"
        );

        BankAccount account = createAccount(
                user,
                "40817810000000000001",
                "1000.00"
        );

        Long accountId = account.getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        Callable<Boolean> withdrawTask = () -> {
            ready.countDown();

            start.await();

            MoneyOperationRequest request =
                    new MoneyOperationRequest();

            request.setAmount(
                    new BigDecimal("800.00")
            );

            try {
                transactionService.withdraw(
                        accountId,
                        user.getEmail(),
                        request
                );

                return true;
            } catch (InvalidOperationException exception) {
                return false;
            }
        };

        try {
            Future<Boolean> first =
                    executor.submit(withdrawTask);

            Future<Boolean> second =
                    executor.submit(withdrawTask);

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS)
            );

            start.countDown();

            boolean firstResult =
                    first.get(10, TimeUnit.SECONDS);

            boolean secondResult =
                    second.get(10, TimeUnit.SECONDS);

            long successfulOperations =
                    (firstResult ? 1 : 0)
                            + (secondResult ? 1 : 0);

            assertEquals(
                    1,
                    successfulOperations
            );

            BankAccount updatedAccount =
                    bankAccountRepository
                            .findById(accountId)
                            .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("200.00")
                            .compareTo(
                                    updatedAccount.getBalance()
                            )
            );

            long withdrawals =
                    transactionRepository
                            .findAll()
                            .stream()
                            .filter(transaction ->
                                    transaction.getType()
                                            == TransactionType.WITHDRAWAL
                            )
                            .count();

            assertEquals(
                    1,
                    withdrawals
            );

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void oppositeTransfersShouldCompleteWithoutDeadlock()
            throws Exception {

        User firstUser = createUser(
                "first@example.com",
                "+79990000001"
        );

        User secondUser = createUser(
                "second@example.com",
                "+79990000002"
        );

        BankAccount firstAccount = createAccount(
                firstUser,
                "40817810000000000001",
                "1000.00"
        );

        BankAccount secondAccount = createAccount(
                secondUser,
                "40817810000000000002",
                "1000.00"
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        Callable<Void> firstTransfer = () -> {
            ready.countDown();

            start.await();

            TransferRequest request =
                    new TransferRequest();

            request.setDestinationAccountNumber(
                    secondAccount.getAccountNumber()
            );

            request.setAmount(
                    new BigDecimal("100.00")
            );

            transferService.transfer(
                    firstAccount.getId(),
                    firstUser.getEmail(),
                    request
            );

            return null;
        };

        Callable<Void> secondTransfer = () -> {
            ready.countDown();

            start.await();

            TransferRequest request =
                    new TransferRequest();

            request.setDestinationAccountNumber(
                    firstAccount.getAccountNumber()
            );

            request.setAmount(
                    new BigDecimal("200.00")
            );

            transferService.transfer(
                    secondAccount.getId(),
                    secondUser.getEmail(),
                    request
            );

            return null;
        };

        try {
            Future<Void> first =
                    executor.submit(firstTransfer);

            Future<Void> second =
                    executor.submit(secondTransfer);

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS)
            );

            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            BankAccount updatedFirst =
                    bankAccountRepository
                            .findById(firstAccount.getId())
                            .orElseThrow();

            BankAccount updatedSecond =
                    bankAccountRepository
                            .findById(secondAccount.getId())
                            .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("1100.00")
                            .compareTo(
                                    updatedFirst.getBalance()
                            )
            );

            assertEquals(
                    0,
                    new BigDecimal("900.00")
                            .compareTo(
                                    updatedSecond.getBalance()
                            )
            );

            BigDecimal totalBalance =
                    updatedFirst.getBalance()
                            .add(updatedSecond.getBalance());

            assertEquals(
                    0,
                    new BigDecimal("2000.00")
                            .compareTo(totalBalance)
            );

            long transfers =
                    transactionRepository
                            .findAll()
                            .stream()
                            .filter(transaction ->
                                    transaction.getType()
                                            == TransactionType.TRANSFER
                            )
                            .count();

            assertEquals(
                    2,
                    transfers
            );

        } finally {
            executor.shutdownNow();
        }
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