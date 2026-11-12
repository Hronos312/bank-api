package ru.bankapi.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
        if (event == null) {
            throw new InvalidBankEventException("Сообщение Kafka не содержит банковского события");
        }
        String trace = event.sourceTraceId();
        if (trace != null && !trace.matches("[0-9a-f]{16}|[0-9a-f]{32}")) {
            trace = null;
        }
        try (var eventContext = MDC.putCloseable("eventId", String.valueOf(event.eventId()));
             var correlation = MDC.putCloseable("correlationId", event.correlationId() == null
                     ? null : event.correlationId().toString());
             var sourceTrace = MDC.putCloseable("sourceTraceId", trace)) {
            processingService.process(event);
            log.debug("Обработка сообщения Kafka завершена. Тема={}, раздел={}, смещение={}",
                    record.topic(), record.partition(), record.offset());
        }
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
