
package ru.bankapi.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final String TEST_TOPIC = "bank.events.dev";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> sendTestMessage(String message) {
        return kafkaTemplate.send(TEST_TOPIC, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Ошибка отправки сообщения в Kafka", exception);
                        return;
                    }

                    log.info(
                            "Сообщение отправлено в Kafka: topic={}, partition={}, offset={}",
                            TEST_TOPIC,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
