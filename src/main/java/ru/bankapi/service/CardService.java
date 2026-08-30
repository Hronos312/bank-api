package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.generator.CardNumberGenerator;
import ru.bankapi.mapper.CardMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.Card;
import ru.bankapi.model.User;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardMapper cardMapper;

    public CardResponse issueCard(Long accountId, String email) {
        User user = getUserByEmail(email);

        BankAccount account = bankAccountRepository
                .findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() ->
                        new NotFoundException("Счёт не найден")
                );

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Карта может быть выпущена только к активному счёту");
        }

        if (cardRepository.existsByBankAccountId(accountId)) {
            throw new InvalidOperationException("К этому счёту уже выпущена карта");
        }

        Card card = new Card();

        card.setCardNumber(generateUniqueCardNumber());
        card.setBankAccount(account);
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setStatus(CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(card);

        return cardMapper.toResponse(savedCard);
    }

    public List<CardResponse> getCards(String email) {
        User user = getUserByEmail(email);

        return cardRepository
                .findAllByBankAccountUserId(user.getId())
                .stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    public CardResponse getCard(Long cardId, String email) {
        User user = getUserByEmail(email);

        Card card = cardRepository
                .findByIdAndBankAccountUserId(cardId, user.getId())
                .orElseThrow(() -> new NotFoundException("Карта не найдена"));

        return cardMapper.toResponse(card);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private String generateUniqueCardNumber() {
        String cardNumber;

        do {
            cardNumber = cardNumberGenerator.generate();
        } while (
                cardRepository.existsByCardNumber(cardNumber)
        );

        return cardNumber;
    }
}