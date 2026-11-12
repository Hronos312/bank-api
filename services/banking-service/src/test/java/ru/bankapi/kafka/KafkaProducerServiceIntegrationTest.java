package ru.bankapi.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DirtiesContext
@SpringBootTest(
        classes = KafkaProducerServiceIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "management.endpoint.health.group.readiness.include=ping"
)
class KafkaProducerServiceIntegrationTest {

    private static final String TOPIC = "bank.events.dev";

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.2");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaProducerService producer;

    @Test
    void shouldSendMessageToKafka() throws Exception {
        String message = "Проверка отправки сообщения";
        var metadata = producer.sendTestMessage(message).get(15, TimeUnit.SECONDS).getRecordMetadata();
        assertEquals(TOPIC, metadata.topic());

        try (var reader = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            var partition = new TopicPartition(TOPIC, metadata.partition());
            reader.assign(List.of(partition));
            reader.seek(partition, metadata.offset());
            var records = reader.poll(Duration.ofSeconds(15)).records(partition);
            assertTrue(!records.isEmpty(), "Kafka не вернула опубликованное сообщение");
            assertEquals(message, records.getFirst().value());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import(KafkaProducerService.class)
    static class TestApplication {
    }
}
