package ru.bankapi.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.model.BankTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
            Long sourceAccountId,
            Long destinationAccountId
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.type = :type
          AND t.sourceAccount.user.id = :userId
        """)
    BigDecimal sumBySourceUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.type = :type
          AND t.sourceAccount.user.id = :userId
          AND t.destinationAccount.user.id <> :userId
        """)
    BigDecimal sumOutgoingTransfersToOtherUsers(@Param("userId") Long userId, @Param("type") TransactionType type);
}