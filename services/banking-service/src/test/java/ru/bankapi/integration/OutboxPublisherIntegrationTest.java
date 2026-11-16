package ru.bankapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.bankapi.dal.OutboxEventRepository;
import ru.bankapi.model.OutboxEvent;
import ru.bankapi.service.outbox.OutboxPublicationStore;
import ru.bankapi.service.outbox.OutboxPublisher;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
class OutboxPublisherIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OutboxEventRepository outbox;

    @Autowired
    private OutboxPublicationStore store;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanOutbox() {
        outbox.deleteAll();
    }

    @Test
    void kafkaWaitLeavesRowUnlockedAndOtherPublisherCanClaimNextEvent() throws Exception {
        UUID first = createEvent();
        UUID second = createEvent();
        jdbc.update("UPDATE outbox_events SET created_at = created_at - INTERVAL '1 second' WHERE id = ?", first);
        var waitingForKafka = new CountDownLatch(1);
        var ack = new CompletableFuture<SendResult<String, String>>();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            waitingForKafka.countDown();
            return ack;
        });
        var publisher = new OutboxPublisher(store, kafka);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var publication = executor.submit(publisher::publishNext);
            try {
                assertThat(waitingForKafka.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(outbox.findById(first).orElseThrow().getPublishedAt()).isNull();
                assertThat(jdbc.queryForObject("SELECT id FROM outbox_events WHERE id = ? FOR UPDATE NOWAIT",
                        UUID.class, first)).isEqualTo(first);
                assertThat(store.claimNext().orElseThrow().id()).isEqualTo(second);
                assertThat(store.claimNext()).isEmpty();
            } finally {
                ack.complete(null);
            }
            publication.get(10, TimeUnit.SECONDS);
        }
        assertThat(outbox.findById(first).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    void expiredLeaseCanBeReclaimedAndOldAttemptCannotMarkItPublished() {
        UUID id = createEvent();
        var oldAttempt = store.claimNext().orElseThrow();
        assertThat(store.claimNext()).isEmpty();

        jdbc.update("UPDATE outbox_events SET lease_until = CURRENT_TIMESTAMP WHERE id = ?", id);
        var newAttempt = store.claimNext().orElseThrow();
        assertThat(newAttempt.id()).isEqualTo(oldAttempt.id());
        assertThat(newAttempt.attempt()).isEqualTo(oldAttempt.attempt() + 1);
        assertThat(store.markPublished(oldAttempt)).isFalse();
        assertThat(store.markPublished(newAttempt)).isTrue();
        assertThat(store.claimNext()).isEmpty();
        assertThat(outbox.findById(id).orElseThrow().getAttempts()).isEqualTo(2);
    }

    private UUID createEvent() {
        var event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTopic("bank.transactions.v1");
        event.setEventKey("account-1");
        event.setEventType("DEPOSIT_COMPLETED");
        event.setPayload(Map.of("eventId", event.getId().toString()));
        return outbox.save(event).getId();
    }
}
