package ru.bankapi.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bankapi.model.BankTransaction;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
}