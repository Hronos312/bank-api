package ru.bankapi.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.CardMapper;
import ru.bankapi.model.Card;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private AdminCardService adminCardService;

    @Test
    void getCardsShouldReturnAllCards() {
        Card first = createCard(1L, CardStatus.ACTIVE);
        Card second = createCard(2L, CardStatus.BLOCKED);

        CardResponse firstResponse = new CardResponse();
        firstResponse.setId(1L);

        CardResponse secondResponse = new CardResponse();
        secondResponse.setId(2L);

        when(cardRepository.findAll())
                .thenReturn(List.of(first, second));

        when(cardMapper.toResponse(first))
                .thenReturn(firstResponse);

        when(cardMapper.toResponse(second))
                .thenReturn(secondResponse);

        List<CardResponse> result =
                adminCardService.getCards();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getCardShouldReturnCard() {
        Card card = createCard(
                1L,
                CardStatus.ACTIVE
        );

        CardResponse response =
                new CardResponse();

        response.setId(1L);
        response.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        when(cardMapper.toResponse(card))
                .thenReturn(response);

        CardResponse result =
                adminCardService.getCard(1L);

        assertEquals(1L, result.getId());

        assertEquals(
                CardStatus.ACTIVE,
                result.getStatus()
        );
    }

    @Test
    void getCardShouldThrowWhenCardDoesNotExist() {
        when(cardRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> adminCardService.getCard(99L)
        );
    }

    @Test
    void blockCardShouldChangeStatusToBlocked() {
        Card card = createCard(
                1L,
                CardStatus.ACTIVE
        );

        CardResponse response =
                new CardResponse();

        response.setId(1L);
        response.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        when(cardMapper.toResponse(card))
                .thenReturn(response);

        CardResponse result =
                adminCardService.blockCard(1L);

        assertEquals(
                CardStatus.BLOCKED,
                card.getStatus()
        );

        assertEquals(
                CardStatus.BLOCKED,
                result.getStatus()
        );
    }

    @Test
    void blockCardShouldThrowWhenAlreadyBlocked() {
        Card card = createCard(
                1L,
                CardStatus.BLOCKED
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.blockCard(1L)
        );

        verify(cardMapper, never())
                .toResponse(any(Card.class));
    }

    @Test
    void blockCardShouldThrowWhenCardIsClosed() {
        Card card = createCard(
                1L,
                CardStatus.CLOSED
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.blockCard(1L)
        );

        assertEquals(
                CardStatus.CLOSED,
                card.getStatus()
        );
    }

    @Test
    void blockCardShouldThrowWhenCardIsExpired() {
        Card card = createCard(
                1L,
                CardStatus.EXPIRED
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.blockCard(1L)
        );
    }

    @Test
    void unblockCardShouldChangeStatusToActive() {
        Card card = createCard(
                1L,
                CardStatus.BLOCKED
        );

        CardResponse response =
                new CardResponse();

        response.setId(1L);
        response.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        when(cardMapper.toResponse(card))
                .thenReturn(response);

        CardResponse result =
                adminCardService.unblockCard(1L);

        assertEquals(
                CardStatus.ACTIVE,
                card.getStatus()
        );

        assertEquals(
                CardStatus.ACTIVE,
                result.getStatus()
        );
    }

    @Test
    void unblockCardShouldThrowWhenAlreadyActive() {
        Card card = createCard(
                1L,
                CardStatus.ACTIVE
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.unblockCard(1L)
        );

        verify(cardMapper, never())
                .toResponse(any(Card.class));
    }

    @Test
    void unblockCardShouldThrowWhenCardIsClosed() {
        Card card = createCard(
                1L,
                CardStatus.CLOSED
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.unblockCard(1L)
        );
    }

    @Test
    void unblockCardShouldThrowWhenCardIsExpired() {
        Card card = createCard(
                1L,
                CardStatus.EXPIRED
        );

        when(cardRepository.findById(1L))
                .thenReturn(Optional.of(card));

        assertThrows(
                InvalidOperationException.class,
                () -> adminCardService.unblockCard(1L)
        );
    }

    private Card createCard(
            Long id,
            CardStatus status
    ) {
        Card card = new Card();

        card.setId(id);
        card.setStatus(status);

        return card;
    }
}