package ru.bankapi.dal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bankapi.model.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByUserId(Long userId);

    Optional<BankAccount> findByIdAndUserId(Long id, Long userId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM BankAccount a
            WHERE a.id = :accountId
              AND a.user.id = :userId
            """)
    Optional<BankAccount> findByIdAndUserIdForUpdate(@Param("accountId") Long accountId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM BankAccount a
            WHERE a.id = :accountId
            """)
    Optional<BankAccount> findByIdForUpdate(@Param("accountId") Long accountId);

    @Query("""
            SELECT a.id
            FROM BankAccount a
            WHERE a.accountNumber = :accountNumber
            """)
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);
}