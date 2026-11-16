package ru.bankapi.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bankapi.model.Card;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    boolean existsByCardNumber(String cardNumber);

    boolean existsByBankAccountId(Long bankAccountId);

    Optional<Card> findByBankAccountId(Long bankAccountId);

    List<Card> findAllByBankAccountUserId(Long userId);

    Optional<Card> findByIdAndBankAccountUserId(Long id, Long userId);
}