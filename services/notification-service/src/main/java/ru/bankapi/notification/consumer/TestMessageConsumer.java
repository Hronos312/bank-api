package ru.bankapi.notification.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TestMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(TestMessageConsumer.class);

    @KafkaListener(topics = "${app.kafka.topic}")
    public void consume(ConsumerRecord<String, String> record) {
        log.info(
                "Received Kafka message: topic={}, partition={}, offset={}, value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.value()
        );
    }
}
