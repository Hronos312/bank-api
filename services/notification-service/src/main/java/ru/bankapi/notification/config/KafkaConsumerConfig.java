package ru.bankapi.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.util.backoff.FixedBackOff;
import ru.bankapi.notification.event.InvalidBankEventException;

import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_STACKTRACE;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic bankTransactionsTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).build();
    }

    @Bean
    public NewTopic deadLetterTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").build();
    }

    @Bean
    public ProducerListener<Object, Object> kafkaProducerListener() {
        var listener = new LoggingProducerListener<Object, Object>();
        listener.setIncludeContents(false);
        return listener;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.excludeHeader(EX_STACKTRACE);

        var handler = new DefaultErrorHandler((record, exception) -> {
            recoverer.accept(record, exception);
            log.warn("Некорректное сообщение передано в DLQ. Тема={}, раздел={}, смещение={}",
                    record.topic(), record.partition(), record.offset());
        }, new FixedBackOff(2000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.addNotRetryableExceptions(InvalidBankEventException.class);
        handler.setRetryListeners((record, exception, attempt) ->
                log.warn("Ошибка обработки сообщения Kafka. Тема={}, раздел={}, смещение={}, попытка={}",
                        record.topic(), record.partition(), record.offset(), attempt));
        return handler;
    }
}
