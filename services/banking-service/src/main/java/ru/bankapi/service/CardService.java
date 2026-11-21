package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dal.UniqueConstraints;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.generator.CardNumberGenerator;
import ru.bankapi.mapper.CardMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.Card;
import ru.bankapi.identity.IdentityDirectory;
import ru.bankapi.identity.IdentityDirectory.UserIdentity;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final int MAX_NUMBER_ATTEMPTS = 5;

    private final CardRepository cardRepository;
    private final BankAccountRepository bankAccountRepository;
    private final IdentityDirectory identityDirectory;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardMapper cardMapper;

    public CardResponse issueCard(Long accountId, String subject) {
        UserIdentity user = getUserBySubject(subject);

        BankAccount account = bankAccountRepository
                .findByIdAndUserId(accountId, user.id())
                .orElseThrow(() ->
                        new NotFoundException("Счёт не найден")
                );

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Карта может быть выпущена только к активному счёту");
        }

        if (cardRepository.existsByBankAccountId(accountId)) {
            throw cardAlreadyIssued();
        }

        LocalDate expirationDate = LocalDate.now().plusYears(3);
        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            Card card = new Card();
            card.setCardNumber(cardNumberGenerator.generate());
            card.setBankAccount(account);
            card.setExpirationDate(expirationDate);
            card.setStatus(CardStatus.ACTIVE);

            Card savedCard;
            try {
                savedCard = cardRepository.saveAndFlush(card);
            } catch (DataIntegrityViolationException exception) {
                if (UniqueConstraints.matches(exception, "cards_bank_account_id_key")) {
                    throw cardAlreadyIssued();
                }
                if (!UniqueConstraints.matches(exception, "cards_card_number_key")) {
                    throw exception;
                }
                continue;
            }
            return cardMapper.toResponse(savedCard);
        }
        if (cardRepository.existsByBankAccountId(accountId)) {
            throw cardAlreadyIssued();
        }
        throw new DuplicateDataException("CARD_NUMBER_CONFLICT",
                "Не удалось подобрать свободный номер карты. Повторите запрос");
    }

    public List<CardResponse> getCards(String subject) {
        UserIdentity user = getUserBySubject(subject);

        return cardRepository
                .findAllByBankAccountUserId(user.id())
                .stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    public CardResponse getCard(Long cardId, String subject) {
        UserIdentity user = getUserBySubject(subject);

        Card card = cardRepository
                .findByIdAndBankAccountUserId(cardId, user.id())
                .orElseThrow(() -> new NotFoundException("Карта не найдена"));

        return cardMapper.toResponse(card);
    }

    private UserIdentity getUserBySubject(String subject) {
        return identityDirectory.findBySubject(subject)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private InvalidOperationException cardAlreadyIssued() {
        return new InvalidOperationException("К этому счёту уже выпущена карта");
    }
}
