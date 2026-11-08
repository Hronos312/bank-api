package ru.bankapi.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.bankapi.dal.OutboxEventRepository;
import ru.bankapi.model.OutboxEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisher publisher;

    @Test
    void shouldMarkEventAsPublishedAfterSuccessfulSend()
            throws Exception {

        OutboxEvent event = createEvent();

        when(outboxRepository.lockNextUnpublished())
                .thenReturn(Optional.of(event));

        when(objectMapper.writeValueAsString(event.getPayload()))
                .thenReturn("{\"message\":\"test\"}");

        when(kafkaTemplate.send(
                event.getTopic(),
                event.getEventKey(),
                "{\"message\":\"test\"}"
        )).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishNext();

        assertEquals(1, event.getAttempts());
        assertNotNull(event.getPublishedAt());

        verify(kafkaTemplate).send(
                event.getTopic(),
                event.getEventKey(),
                "{\"message\":\"test\"}"
        );
    }

    @Test
    void shouldKeepEventPendingWhenKafkaFails()
            throws Exception {

        OutboxEvent event = createEvent();

        when(outboxRepository.lockNextUnpublished())
                .thenReturn(Optional.of(event));

        when(objectMapper.writeValueAsString(event.getPayload()))
                .thenReturn("{\"message\":\"test\"}");

        when(kafkaTemplate.send(
                event.getTopic(),
                event.getEventKey(),
                "{\"message\":\"test\"}"
        )).thenReturn(
                CompletableFuture.failedFuture(
                        new IllegalStateException("Kafka недоступна")
                )
        );

        publisher.publishNext();

        assertEquals(1, event.getAttempts());
        assertNull(event.getPublishedAt());
    }

    @Test
    void shouldSkipWhenNoPendingEvents() {
        when(outboxRepository.lockNextUnpublished())
                .thenReturn(Optional.empty());

        publisher.publishNext();

        verifyNoInteractions(kafkaTemplate, objectMapper);
    }

    private OutboxEvent createEvent() {
        OutboxEvent event = new OutboxEvent();

        event.setId(UUID.randomUUID());
        event.setTopic("bank.events.dev");
        event.setEventKey("test-1");
        event.setEventType("OUTBOX_TEST");
        event.setPayload(Map.of("message", "test"));

        return event;
    }
}
