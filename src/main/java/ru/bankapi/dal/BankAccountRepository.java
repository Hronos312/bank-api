package ru.bankapi.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bankapi.model.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByUserId(Long userId);

    Optional<BankAccount> findByIdAndUserId(Long id, Long userId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}