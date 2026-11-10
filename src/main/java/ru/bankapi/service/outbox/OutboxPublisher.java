package ru.bankapi.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bankapi.service.outbox.OutboxPublicationStore.Publication;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxPublicationStore publicationStore;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.poll-delay-ms:5000}")
    public void publishNext() {
        publicationStore.claimNext().ifPresent(this::publish);
    }

    private void publish(Publication event) {
        try {
            kafkaTemplate.send(event.topic(), event.key(), event.payload()).get(20, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Публикация события прервана. Событие={}, попытка={}", event.id(), event.attempt());
            return;
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            // При сбое событие снова доступно после истечения аренды.
            log.warn("Не удалось опубликовать событие в Kafka. Событие={}, попытка={}, ошибка={}",
                    event.id(), event.attempt(), exception.getClass().getSimpleName());
            return;
        }

        if (publicationStore.markPublished(event)) {
            log.info("Событие опубликовано в Kafka. Событие={}, тема={}", event.id(), event.topic());
        } else {
            log.warn("Подтверждение Kafka получено после перехвата события. Событие={}, попытка={}",
                    event.id(), event.attempt());
        }
    }
}
