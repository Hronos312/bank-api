package ru.bankapi.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.notification.event.BankTransactionCompletedEvent;
import ru.bankapi.notification.event.InvalidBankEventException;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "DEPOSIT_COMPLETED",
            "WITHDRAWAL_COMPLETED",
            "TRANSFER_COMPLETED"
    );

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void process(BankTransactionCompletedEvent event) {
        BigDecimal amount = validate(event);

        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_events (event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId()
        );

        if (inserted == 0) {
            log.debug(
                    "Событие уже обработано, повторное уведомление не создаётся: id={}",
                    event.eventId()
            );
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO notifications (
                    event_id,
                    transaction_id,
                    event_type,
                    source_account_id,
                    destination_account_id,
                    amount,
                    currency
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                event.eventId(),
                event.transactionId(),
                event.eventType(),
                event.sourceAccountId(),
                event.destinationAccountId(),
                amount,
                event.currency()
        );
    }

    private BigDecimal validate(BankTransactionCompletedEvent event) {
        if (event == null
                || event.eventId() == null
                || event.transactionId() == null
                || event.transactionId() <= 0
                || event.eventType() == null
                || event.amount() == null
                || event.occurredAt() == null
                || event.eventVersion() != 1
                || !SUPPORTED_EVENTS.contains(event.eventType())
                || !"RUB".equals(event.currency())) {

            throw new InvalidBankEventException("Получено некорректное банковское событие");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(event.amount());
        } catch (NumberFormatException exception) {
            throw new InvalidBankEventException("Некорректная сумма банковской операции");
        }

        // NUMERIC(19, 2) допускает до 17 цифр перед десятичной точкой.
        if (amount.signum() <= 0 || amount.scale() > 2 || (long) amount.precision() - amount.scale() > 17) {
            throw new InvalidBankEventException("Некорректная сумма банковской операции");
        }
        return amount;
    }
}
