package ru.bankapi.kafka.event;

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
        Instant occurredAt
) {
}
