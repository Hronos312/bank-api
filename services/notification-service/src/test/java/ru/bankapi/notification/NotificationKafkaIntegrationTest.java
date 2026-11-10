package ru.bankapi.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import ru.bankapi.notification.event.BankTransactionCompletedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class NotificationKafkaIntegrationTest {

    private static final String TOPIC = "bank.transactions.v1";
    private static final String GROUP = "notification-service-test";
    private static final String DLQ_FAILURE_KEY = "oversized-dlq-record";
    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.2");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> GROUP);
    }

    @Autowired
    private KafkaTemplate<String, String> producer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void duplicateDeliveryCreatesOneNotification() throws Exception {
        var event = event(1L, "100.50");
        String message = objectMapper.writeValueAsString(event);
        send(event.eventId().toString(), message);
        long duplicateOffset = send(event.eventId().toString(), message);

        awaitCommit(duplicateOffset);
        assertRows(event.eventId(), 1);
    }

    @Test
    void insertFailureRollsBackDeduplicationAndKafkaRetries() throws Exception {
        var event = event(42L, "100.50");
        jdbc.execute("CREATE SEQUENCE test_insert_attempt");
        jdbc.execute("""
                CREATE FUNCTION test_fail_insert() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.transaction_id = 42 THEN
                        PERFORM nextval('test_insert_attempt');
                        RAISE EXCEPTION 'Тестовый отказ записи уведомления';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER test_fail_insert BEFORE INSERT ON notifications
                FOR EACH ROW EXECUTE FUNCTION test_fail_insert()
                """);

        long offset;
        try {
            offset = send(event.eventId().toString(), objectMapper.writeValueAsString(event));
            await().atMost(TIMEOUT).untilAsserted(() -> {
                assertThat(jdbc.queryForObject("SELECT is_called FROM test_insert_attempt", Boolean.class)).isTrue();
                assertThat(jdbc.queryForObject("SELECT last_value FROM test_insert_attempt", Long.class))
                        .isGreaterThanOrEqualTo(2L);
            });
            assertRows(event.eventId(), 0);
            try (Admin admin = kafkaAdmin()) {
                assertThat(committedOffset(admin)).isLessThanOrEqualTo(offset);
            }
        } finally {
            jdbc.execute("DROP TRIGGER test_fail_insert ON notifications");
            jdbc.execute("DROP FUNCTION test_fail_insert()");
            jdbc.execute("DROP SEQUENCE test_insert_attempt");
        }

        awaitCommit(offset);
        assertRows(event.eventId(), 1);
    }

    @Test
    void malformedAndInvalidEventsGoToDlqAndDoNotBlockThePartition() throws Exception {
        var invalid = event(2L, "100000000000000000.00");
        var valid = event(3L, "2.00");
        String invalidJson = objectMapper.writeValueAsString(invalid);
        send("broken-json", "{");
        send("missing-payload", null);
        send("invalid-amount", invalidJson);
        long validOffset = send(valid.eventId().toString(), objectMapper.writeValueAsString(valid));

        awaitCommit(validOffset);
        assertRows(valid.eventId(), 1);
        assertRows(invalid.eventId(), 0);

        try (var reader = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            var partition = new TopicPartition(TOPIC + ".DLT", 0);
            reader.assign(List.of(partition));
            reader.seekToBeginning(List.of(partition));
            List<String> keys = new ArrayList<>();
            List<String> values = new ArrayList<>();
            await().atMost(TIMEOUT).untilAsserted(() -> {
                reader.poll(Duration.ofMillis(250)).forEach(record -> {
                    if (!DLQ_FAILURE_KEY.equals(record.key())) {
                        keys.add(record.key());
                        values.add(record.value());
                    }
                });
                assertThat(keys).containsExactly("broken-json", "missing-payload", "invalid-amount");
                assertThat(values).containsExactly("{", null, invalidJson);
            });
        }
    }

    @Test
    void failedDlqPublishDoesNotCommitOriginalOffset(CapturedOutput output) throws Exception {
        String message = "{" + "x".repeat(4096);
        var dlq = new ConfigResource(ConfigResource.Type.TOPIC, TOPIC + ".DLT");
        try (Admin admin = kafkaAdmin()) {
            changeDlqLimit(admin, dlq, "1024", AlterConfigOp.OpType.SET);
            long offset;
            try {
                offset = send(DLQ_FAILURE_KEY, message);
                await().atMost(TIMEOUT).untilAsserted(() ->
                        assertThat(output.getAll()).contains("RecordTooLargeException").doesNotContain(message));
                assertThat(committedOffset(admin)).isLessThanOrEqualTo(offset);
            } finally {
                changeDlqLimit(admin, dlq, null, AlterConfigOp.OpType.DELETE);
            }
            awaitCommit(offset);
        }
    }

    private void changeDlqLimit(Admin admin, ConfigResource dlq, String value, AlterConfigOp.OpType type)
            throws Exception {
        var operation = new AlterConfigOp(new ConfigEntry("max.message.bytes", value), type);
        admin.incrementalAlterConfigs(Map.of(dlq, List.of(operation))).all().get(10, TimeUnit.SECONDS);
    }

    private BankTransactionCompletedEvent event(long transactionId, String amount) {
        return new BankTransactionCompletedEvent(UUID.randomUUID(), "DEPOSIT_COMPLETED", 1,
                transactionId, null, 1L, amount, "RUB", Instant.now());
    }

    private long send(String key, String message) throws Exception {
        return producer.send(TOPIC, key, message).get(15, TimeUnit.SECONDS).getRecordMetadata().offset();
    }

    private void awaitCommit(long offset) {
        try (Admin admin = kafkaAdmin()) {
            await().atMost(TIMEOUT).untilAsserted(() -> assertThat(committedOffset(admin)).isGreaterThan(offset));
        }
    }

    private Admin kafkaAdmin() {
        return Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    }

    private long committedOffset(Admin admin) throws Exception {
        var offsets = admin.listConsumerGroupOffsets(GROUP).partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
        var committed = offsets.get(new TopicPartition(TOPIC, 0));
        return committed == null ? -1L : committed.offset();
    }

    private void assertRows(UUID eventId, int count) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events WHERE event_id = ?", Integer.class,
                eventId)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE event_id = ?", Integer.class,
                eventId)).isEqualTo(count);
    }
}
