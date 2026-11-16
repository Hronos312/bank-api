package ru.bankapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerListener<Object, Object> kafkaProducerListener() {
        var listener = new LoggingProducerListener<Object, Object>();
        listener.setIncludeContents(false);
        return listener;
    }
}
