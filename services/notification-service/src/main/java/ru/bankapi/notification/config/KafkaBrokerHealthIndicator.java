package ru.bankapi.notification.config;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.Admin;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component("kafkaBroker")
class KafkaBrokerHealthIndicator implements HealthIndicator {

    private final Admin admin;

    KafkaBrokerHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.admin = Admin.create(kafkaAdmin.getConfigurationProperties());
    }

    @Override
    public Health health() {
        try {
            return admin.describeCluster().nodes().get(1, TimeUnit.SECONDS).isEmpty()
                    ? Health.down().build() : Health.up().build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Health.down().build();
        } catch (ExecutionException | TimeoutException exception) {
            return Health.down().build();
        }
    }

    @PreDestroy
    void close() {
        admin.close(Duration.ofSeconds(1));
    }
}
