
package ru.bankapi.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = KafkaProducerServiceIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@EmbeddedKafka(
        partitions = 1,
        topics = "bank.events.dev",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class KafkaProducerServiceIntegrationTest {

    private static final String TOPIC = "bank.events.dev";

    @Autowired
    private KafkaProducerService producerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldSendMessageToKafka() throws Exception {
        String message = "Hello from Spring Boot!";

        var consumerProperties = KafkaTestUtils.consumerProps(
                "bank-api-test",
                "false",
                embeddedKafka
        );

        var consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        );

        try (Consumer<String, String> consumer =
                     consumerFactory.createConsumer()) {

            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

            SendResult<String, String> result = producerService
                    .sendTestMessage(message)
                    .get(10, TimeUnit.SECONDS);

            assertEquals(TOPIC, result.getRecordMetadata().topic());

            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(
                            consumer,
                            TOPIC,
                            Duration.ofSeconds(10)
                    );

            assertEquals(message, record.value());
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
