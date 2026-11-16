package ru.bankapi.service.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.bankapi.service.outbox.OutboxPublicationStore.Publication;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private final Publication event = new Publication(UUID.randomUUID(), "bank.transactions.v1",
            "account-1", "{\"message\":\"test\"}", 1);

    @Mock
    private OutboxPublicationStore publicationStore;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher publisher;

    @Test
    void shouldMarkEventAsPublishedAfterSuccessfulSend() {
        when(publicationStore.claimNext()).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.topic(), event.key(), event.payload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(publicationStore.markPublished(event)).thenReturn(true);

        publisher.publishNext();

        var order = inOrder(publicationStore, kafkaTemplate);
        order.verify(publicationStore).claimNext();
        order.verify(kafkaTemplate).send(event.topic(), event.key(), event.payload());
        order.verify(publicationStore).markPublished(event);
    }

    @Test
    void shouldKeepEventPendingWhenKafkaFails() {
        when(publicationStore.claimNext()).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.topic(), event.key(), event.payload()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka недоступна")));

        publisher.publishNext();

        verify(publicationStore, never()).markPublished(event);
    }

    @Test
    void shouldSkipWhenNoPendingEvents() {
        when(publicationStore.claimNext()).thenReturn(Optional.empty());

        publisher.publishNext();

        verifyNoInteractions(kafkaTemplate);
    }
}
