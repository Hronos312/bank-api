package ru.bankapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dal.OutboxEventRepository;
import org.springframework.context.annotation.Import;
import ru.bankapi.fixture.TestIdentityConfiguration;
import ru.bankapi.fixture.TestIdentityDirectory;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.service.MoneyIdempotencyService;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.OutboxEvent;
import ru.bankapi.fixture.TestUser;
import ru.bankapi.service.TransactionService;
import ru.bankapi.service.TransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@Import(TestIdentityConfiguration.class)
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
class BankingOutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("bank_api_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TestIdentityDirectory identityDirectory;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MoneyIdempotencyService idempotency;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        identityDirectory.deleteAll();
    }

    @Test
    void depositShouldCreateOutboxEvent() {
        TestUser user = createUser("ivan@test.com", "+79990000001");
        BankAccount account = createAccount(
                user, "40817810000000000001", "1000.00"
        );

        MoneyOperationRequest request =
                moneyRequest("500.00");

        TransactionResponse response = transactionService.deposit(
                account.getId(), user.getEmail(), request
        );

        assertEquals(1, transactionRepository.count());
        assertBalance(account.getId(), "1500.00");

        assertEvent(
                "DEPOSIT_COMPLETED",
                response.getId(),
                null,
                account.getId(),
                "500.00"
        );
    }

    @Test
    void concurrentRetryShouldCreateOneTransactionAndRejectDifferentRequest() throws Exception {
        TestUser user = createUser("ivan@test.com", "+79990000001");
        BankAccount account = createAccount(user, "40817810000000000001", "1000.00");
        var request = moneyRequest("50.00");
        var operation = new MoneyIdempotencyService.Operation(
                TransactionType.DEPOSIT, account.getId(), request.getAmount(), null, null);
        String key = UUID.randomUUID().toString();
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<TransactionResponse> call = () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return idempotency.execute(user.getId(), key, operation,
                        () -> transactionService.deposit(account.getId(), user.getEmail(), request));
            };
            var first = workers.submit(call);
            var second = workers.submit(call);
            start.countDown();
            assertEquals(first.get(10, TimeUnit.SECONDS).getId(), second.get(10, TimeUnit.SECONDS).getId());
        }

        assertBalance(account.getId(), "1050.00");
        assertEquals(1, transactionRepository.count());
        assertEquals(1, outboxRepository.count());
        var different = new MoneyIdempotencyService.Operation(
                TransactionType.DEPOSIT, account.getId(), new BigDecimal("55.00"), null, null);
        assertThrows(DuplicateDataException.class, () -> idempotency.execute(user.getId(), key, different,
                () -> fail("Повтор с другим содержимым не должен исполняться")));
    }

    @Test
    void withdrawalShouldCreateOutboxEvent() {
        TestUser user = createUser("ivan@test.com", "+79990000001");
        BankAccount account = createAccount(
                user, "40817810000000000001", "1000.00"
        );

        TransactionResponse response = transactionService.withdraw(
                account.getId(),
                user.getEmail(),
                moneyRequest("250.00")
        );

        assertEquals(1, transactionRepository.count());
        assertBalance(account.getId(), "750.00");

        assertEvent(
                "WITHDRAWAL_COMPLETED",
                response.getId(),
                account.getId(),
                null,
                "250.00"
        );
    }

    @Test
    void transferShouldCreateSingleOutboxEvent() {
        TestUser sender = createUser("ivan@test.com", "+79990000001");
        TestUser receiver = createUser("petr@test.com", "+79990000002");

        BankAccount source = createAccount(
                sender, "40817810000000000001", "1000.00"
        );
        BankAccount destination = createAccount(
                receiver, "40817810000000000002", "200.00"
        );

        TransferRequest request = new TransferRequest();
        request.setDestinationAccountNumber(
                destination.getAccountNumber()
        );
        request.setAmount(new BigDecimal("300.00"));

        TransactionResponse response = transferService.transfer(
                source.getId(), sender.getEmail(), request
        );

        assertBalance(source.getId(), "700.00");
        assertBalance(destination.getId(), "500.00");
        assertEquals(1, transactionRepository.count());

        assertEvent(
                "TRANSFER_COMPLETED",
                response.getId(),
                source.getId(),
                destination.getId(),
                "300.00"
        );
    }

    @Test
    void rollbackShouldUndoBalanceTransactionAndOutbox() {
        TestUser user = createUser("ivan@test.com", "+79990000001");
        BankAccount account = createAccount(
                user, "40817810000000000001", "1000.00"
        );

        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () ->
                template.execute(status -> {

                    transactionService.withdraw(
                            account.getId(),
                            user.getEmail(),
                            moneyRequest("200.00")
                    );

                    // Отправляем SQL в PostgreSQL до отката.
                    accountRepository.flush();
                    transactionRepository.flush();
                    outboxRepository.flush();

                    throw new IllegalStateException(
                            "Simulated failure"
                    );
                })
        );

        assertBalance(account.getId(), "1000.00");
        assertEquals(0, transactionRepository.count());
        assertEquals(0, outboxRepository.count());
    }

    private void assertEvent(
            String expectedType,
            Long transactionId,
            Long sourceId,
            Long destinationId,
            String amount
    ) {
        List<OutboxEvent> events = outboxRepository.findAll();

        assertEquals(1, events.size());

        OutboxEvent event = events.get(0);

        assertEquals("bank.transactions.v1", event.getTopic());
        assertEquals(expectedType, event.getEventType());
        assertNull(event.getPublishedAt());

        var payload = event.getPayload();

        assertEquals(expectedType, payload.get("eventType"));
        assertEquals(
                transactionId,
                numberAsLong(payload.get("transactionId"))
        );
        assertEquals(
                sourceId,
                numberAsLong(payload.get("sourceAccountId"))
        );
        assertEquals(
                destinationId,
                numberAsLong(payload.get("destinationAccountId"))
        );
        assertEquals(amount, payload.get("amount"));
        assertEquals("RUB", payload.get("currency"));
        assertEquals(event.getId().toString(), payload.get("eventId"));
    }

    private Long numberAsLong(Object value) {
        return value == null
                ? null
                : ((Number) value).longValue();
    }

    private void assertBalance(Long accountId, String expected) {
        BigDecimal balance = accountRepository.findById(accountId)
                .orElseThrow()
                .getBalance();

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(balance)
        );
    }

    private MoneyOperationRequest moneyRequest(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TestUser createUser(String email, String phone) {
        TestUser user = new TestUser();
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        return identityDirectory.save(user);
    }

    private BankAccount createAccount(
            TestUser user,
            String number,
            String balance
    ) {
        BankAccount account = new BankAccount();
        account.setUserId(user.getId());
        account.setAccountNumber(number);
        account.setBalance(new BigDecimal(balance));

        return accountRepository.save(account);
    }
}
