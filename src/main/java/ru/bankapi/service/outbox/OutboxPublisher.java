package ru.bankapi.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.OutboxEventRepository;
import ru.bankapi.model.OutboxEvent;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.poll-delay-ms:5000}")
    @Transactional
    public void publishNext() {
        outboxRepository.lockNextUnpublished().ifPresent(this::publish);
    }

    private void publish(OutboxEvent event) {
        event.setAttempts(event.getAttempts() + 1);

        try {
            String message = objectMapper.writeValueAsString(event.getPayload());

            kafkaTemplate.send(
                    event.getTopic(),
                    event.getEventKey(),
                    message
            ).get(20, TimeUnit.SECONDS);

            event.setPublishedAt(OffsetDateTime.now());

            log.info(
                    "Событие успешно опубликовано в Kafka: id={}, тип={}, topic={}",
                    event.getId(),
                    event.getEventType(),
                    event.getTopic()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            log.warn("Поток публикации события был прерван: id={}", event.getId(), exception);

        } catch (Exception exception) {
            log.warn(
                    "Не удалось опубликовать событие в Kafka: id={}, тип={}, попытка={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAttempts(),
                    exception
            );
        }
    }
}
