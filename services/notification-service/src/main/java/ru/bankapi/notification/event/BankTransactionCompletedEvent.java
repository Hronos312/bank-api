package ru.bankapi.notification.event;

import java.time.Instant;
import java.util.UUID;

public record BankTransactionCompletedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Long transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        String amount,
        String currency,
        Instant occurredAt,
        UUID correlationId,
        String sourceTraceId
) {
    public BankTransactionCompletedEvent(UUID eventId, String eventType, int eventVersion, Long transactionId,
                                         Long sourceAccountId, Long destinationAccountId,
                                         String amount, String currency, Instant occurredAt) {
        this(eventId, eventType, eventVersion, transactionId, sourceAccountId, destinationAccountId,
                amount, currency, occurredAt, null, null);
    }
}
