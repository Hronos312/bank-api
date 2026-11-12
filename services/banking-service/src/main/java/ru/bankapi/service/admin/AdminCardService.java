package ru.bankapi.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.CardMapper;
import ru.bankapi.model.Card;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCardService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public List<CardResponse> getCards() {
        return cardRepository.findAll()
                .stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long cardId) {
        Card card = getCardById(cardId);

        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = getCardById(cardId);

        if (card.getStatus() == CardStatus.CLOSED) {
            throw new InvalidOperationException("Закрытую карту нельзя заблокировать");
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new InvalidOperationException("Истёкшую карту нельзя заблокировать");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new InvalidOperationException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCKED);

        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse unblockCard(Long cardId) {
        Card card = getCardById(cardId);

        if (card.getStatus() == CardStatus.CLOSED) {
            throw new InvalidOperationException("Закрытую карту нельзя разблокировать");
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new InvalidOperationException("Истёкшую карту нельзя разблокировать");
        }

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new InvalidOperationException("Карта уже активна");
        }

        card.setStatus(CardStatus.ACTIVE);

        return cardMapper.toResponse(card);
    }

    private Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Карта не найдена"));
    }
}