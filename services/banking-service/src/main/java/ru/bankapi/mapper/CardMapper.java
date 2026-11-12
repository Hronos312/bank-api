package ru.bankapi.mapper;

import org.springframework.stereotype.Component;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.model.Card;

@Component
public class CardMapper {

    public CardResponse toResponse(Card card) {
        CardResponse response = new CardResponse();

        response.setId(card.getId());
        response.setCardNumber(card.getCardNumber());
        response.setAccountId(card.getBankAccount().getId());
        response.setExpirationDate(card.getExpirationDate());
        response.setStatus(card.getStatus());
        response.setCreatedAt(card.getCreatedAt());

        return response;
    }
}