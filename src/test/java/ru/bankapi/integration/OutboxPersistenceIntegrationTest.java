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
import ru.bankapi.model.OutboxEvent;
import ru.bankapi.service.outbox.OutboxService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
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
                outboxService.record(
                        "account-10",
                        "DEPOSIT_COMPLETED",
                        Map.of(
                                "transactionId", 100L,
                                "amount", "500.00",
                                "currency", "RUB"
                        )
                )
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
            UUID id = outboxService.record(
                    "account-10",
                    "WITHDRAWAL_COMPLETED",
                    Map.of("amount", "200.00")
            );

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
                () -> outboxService.record(
                        "account-10",
                        "DEPOSIT_COMPLETED",
                        Map.of("amount", "100.00")
                )
        );
    }
}
