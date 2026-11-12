package ru.bankapi.service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxPublicationStore {

    private final JdbcClient jdbcClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Publication> claimNext() {
        return jdbcClient.sql("""
                WITH next_event AS (
                    SELECT id
                    FROM outbox_events
                    WHERE published_at IS NULL
                      AND (lease_until IS NULL OR lease_until <= CURRENT_TIMESTAMP)
                    ORDER BY created_at, id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events AS event
                SET lease_until = clock_timestamp() + INTERVAL '1 minute',
                    attempts = event.attempts + 1
                FROM next_event
                WHERE event.id = next_event.id
                RETURNING event.id, event.topic, event.event_key, event.payload::text, event.attempts
                """)
                .query((row, index) -> new Publication(
                        row.getObject("id", UUID.class),
                        row.getString("topic"),
                        row.getString("event_key"),
                        row.getString("payload"),
                        row.getInt("attempts")))
                .optional();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markPublished(Publication event) {
        // Номер попытки защищает запись от запоздавшего подтверждения старого издателя.
        return jdbcClient.sql("""
                UPDATE outbox_events
                SET published_at = clock_timestamp(), lease_until = NULL
                WHERE id = :id AND attempts = :attempt AND published_at IS NULL
                """)
                .param("id", event.id())
                .param("attempt", event.attempt())
                .update() == 1;
    }

    public record Publication(UUID id, String topic, String key, String payload, int attempt) {
    }
}
