package ru.bankapi.service.outbox;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.OutboxEventRepository;
import ru.bankapi.kafka.event.BankTransactionCompletedEvent;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.BankTransaction;
import ru.bankapi.model.OutboxEvent;
import ru.bankapi.observability.CorrelationFilter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String TRANSACTION_TOPIC = "bank.transactions.v1";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(String eventKey, String eventType, Map<String, Object> payload) {
        return saveEvent(UUID.randomUUID(), eventKey, eventType, new HashMap<>(payload));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID recordBankTransaction(BankTransaction transaction) {

        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Транзакция должна быть сохранена перед созданием события");
        }

        BankAccount source = transaction.getSourceAccount();
        BankAccount destination = transaction.getDestinationAccount();

        Long sourceId = source == null ? null : source.getId();
        Long destinationId = destination == null ? null : destination.getId();

        String eventType = transaction.getType().name() + "_COMPLETED";

        UUID eventId = UUID.randomUUID();

        BankTransactionCompletedEvent event = new BankTransactionCompletedEvent(
                eventId,
                eventType,
                1,
                transaction.getId(),
                sourceId,
                destinationId,
                transaction.getAmount().toPlainString(),
                "RUB",
                Instant.now()
        );

        Map<String, Object> payload = objectMapper.convertValue(event, new TypeReference<Map<String, Object>>() {});
        String correlationId = MDC.get(CorrelationFilter.MDC_KEY);
        if (correlationId != null) {
            payload.put("correlationId", correlationId);
        }
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            payload.put("sourceTraceId", traceId);
        }

        Long accountId = sourceId != null ? sourceId : destinationId;

        return saveEvent(eventId, "account-" + accountId, eventType, payload);
    }

    private UUID saveEvent(UUID eventId, String eventKey, String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();

        event.setId(eventId);
        event.setTopic(TRANSACTION_TOPIC);
        event.setEventKey(eventKey);
        event.setEventType(eventType);
        event.setPayload(payload);

        outboxEventRepository.save(event);

        return eventId;
    }
}
