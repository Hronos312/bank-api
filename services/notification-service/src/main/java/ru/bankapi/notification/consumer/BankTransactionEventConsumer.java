package ru.bankapi.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.bankapi.notification.event.BankTransactionCompletedEvent;
import ru.bankapi.notification.event.InvalidBankEventException;
import ru.bankapi.notification.service.NotificationProcessingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransactionEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService processingService;

    @KafkaListener(topics = "${app.kafka.topic}")
    public void consume(ConsumerRecord<String, String> record) {
        BankTransactionCompletedEvent event = readEvent(record.value());
        processingService.process(event);

        log.debug(
                "Обработка сообщения Kafka завершена: topic={}, partition={}, offset={}, eventId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                event.eventId()
        );
    }

    private BankTransactionCompletedEvent readEvent(String value) {
        if (value == null) {
            throw new InvalidBankEventException("Сообщение Kafka не содержит банковского события");
        }

        try {
            return objectMapper.readerFor(BankTransactionCompletedEvent.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(value);
        } catch (JsonProcessingException exception) {
            throw new InvalidBankEventException("Не удалось прочитать JSON банковского события");
        }
    }
}
