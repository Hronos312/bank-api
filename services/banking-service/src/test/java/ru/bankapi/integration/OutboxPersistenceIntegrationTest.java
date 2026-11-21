package ru.bankapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.bankapi.dal.OutboxEventRepository;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.BankTransaction;
import ru.bankapi.model.OutboxEvent;
import ru.bankapi.service.outbox.OutboxService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class OutboxPersistenceIntegrationTest {

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
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldPersistEventAfterCommit() {
        UUID eventId = transactionTemplate.execute(status ->
                outboxService.recordBankTransaction(deposit())
        );

        assertNotNull(eventId);

        OutboxEvent saved = outboxRepository
                .findById(eventId)
                .orElseThrow();

        assertEquals("bank.transactions.v1", saved.getTopic());
        assertEquals("account-10", saved.getEventKey());
        assertEquals("DEPOSIT_COMPLETED", saved.getEventType());
        assertEquals("500.00", saved.getPayload().get("amount"));
        assertNull(saved.getPublishedAt());
        assertEquals(0, saved.getAttempts());
    }

    @Test
    void shouldRollbackEventWhenTransactionFails() {
        UUID eventId = transactionTemplate.execute(status -> {
            UUID id = outboxService.recordBankTransaction(deposit());

            outboxRepository.flush();

            status.setRollbackOnly();

            return id;
        });

        assertNotNull(eventId);
        assertFalse(outboxRepository.existsById(eventId));
    }

    @Test
    void shouldRejectRecordWithoutActiveTransaction() {
        assertThrows(
                IllegalTransactionStateException.class,
                () -> outboxService.recordBankTransaction(deposit())
        );
    }

    private BankTransaction deposit() {
        BankAccount account = new BankAccount();
        account.setId(10L);

        BankTransaction transaction = new BankTransaction();
        transaction.setId(100L);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setDestinationAccount(account);
        transaction.setAmount(new BigDecimal("500.00"));
        return transaction;
    }
}
