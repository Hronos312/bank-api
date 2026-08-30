package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    @Test
    void issueCardShouldCreateActiveCardForCurrentUserAccount() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setUser(user);
        account.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        when(cardRepository.existsByBankAccountId(10L))
                .thenReturn(false);

        when(cardNumberGenerator.generate())
                .thenReturn("2200123456789012");

        when(cardRepository.existsByCardNumber(
                "2200123456789012"
        )).thenReturn(false);

        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation -> {
                    Card card = invocation.getArgument(0);
                    card.setId(20L);
                    return card;
                });

        CardResponse response = new CardResponse();
        response.setId(20L);
        response.setCardNumber("2200123456789012");
        response.setAccountId(10L);
        response.setExpirationDate(
                LocalDate.now().plusYears(3)
        );
        response.setStatus(CardStatus.ACTIVE);

        when(cardMapper.toResponse(any(Card.class)))
                .thenReturn(response);

        CardResponse result =
                cardService.issueCard(10L, email);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals(
                "2200123456789012",
                result.getCardNumber()
        );
        assertEquals(10L, result.getAccountId());
        assertEquals(
                CardStatus.ACTIVE,
                result.getStatus()
        );

        ArgumentCaptor<Card> captor =
                ArgumentCaptor.forClass(Card.class);

        verify(cardRepository).save(captor.capture());

        Card savedCard = captor.getValue();

        assertEquals(
                "2200123456789012",
                savedCard.getCardNumber()
        );
        assertEquals(account, savedCard.getBankAccount());
        assertEquals(
                CardStatus.ACTIVE,
                savedCard.getStatus()
        );
        assertEquals(
                LocalDate.now().plusYears(3),
                savedCard.getExpirationDate()
        );
    }

    @Test
    void issueCardShouldGenerateAnotherNumberWhenFirstAlreadyExists() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setUser(user);
        account.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        when(cardRepository.existsByBankAccountId(10L))
                .thenReturn(false);

        when(cardNumberGenerator.generate())
                .thenReturn(
                        "2200123456789012",
                        "2200987654321098"
                );

        when(cardRepository.existsByCardNumber(
                "2200123456789012"
        )).thenReturn(true);

        when(cardRepository.existsByCardNumber(
                "2200987654321098"
        )).thenReturn(false);

        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(cardMapper.toResponse(any(Card.class)))
                .thenReturn(new CardResponse());

        cardService.issueCard(10L, email);

        verify(cardNumberGenerator, times(2))
                .generate();

        verify(cardRepository)
                .existsByCardNumber(
                        "2200123456789012"
                );

        verify(cardRepository)
                .existsByCardNumber(
                        "2200987654321098"
                );
    }

    @Test
    void issueCardShouldThrowWhenAccountDoesNotBelongToCurrentUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                99L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> cardService.issueCard(
                        99L,
                        email
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardNumberGenerator, never())
                .generate();
    }

    @Test
    void issueCardShouldThrowWhenAccountIsBlocked() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setUser(user);
        account.setStatus(AccountStatus.BLOCKED);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> cardService.issueCard(
                        10L,
                        email
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardNumberGenerator, never())
                .generate();
    }

    @Test
    void issueCardShouldThrowWhenAccountAlreadyHasCard() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setUser(user);
        account.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(account));

        when(cardRepository.existsByBankAccountId(10L))
                .thenReturn(true);

        assertThrows(
                InvalidOperationException.class,
                () -> cardService.issueCard(
                        10L,
                        email
                )
        );

        verify(cardRepository, never())
                .save(any(Card.class));

        verify(cardNumberGenerator, never())
                .generate();
    }

    @Test
    void getCardsShouldReturnOnlyCurrentUserCards() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        Card firstCard = new Card();
        firstCard.setId(20L);

        Card secondCard = new Card();
        secondCard.setId(21L);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(cardRepository.findAllByBankAccountUserId(1L))
                .thenReturn(
                        List.of(firstCard, secondCard)
                );

        CardResponse firstResponse =
                new CardResponse();
        firstResponse.setId(20L);

        CardResponse secondResponse =
                new CardResponse();
        secondResponse.setId(21L);

        when(cardMapper.toResponse(firstCard))
                .thenReturn(firstResponse);

        when(cardMapper.toResponse(secondCard))
                .thenReturn(secondResponse);

        List<CardResponse> result =
                cardService.getCards(email);

        assertEquals(2, result.size());
        assertEquals(20L, result.get(0).getId());
        assertEquals(21L, result.get(1).getId());

        verify(cardRepository)
                .findAllByBankAccountUserId(1L);
    }

    @Test
    void getCardShouldReturnCardOwnedByCurrentUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        Card card = new Card();
        card.setId(20L);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(cardRepository.findByIdAndBankAccountUserId(
                20L,
                1L
        )).thenReturn(Optional.of(card));

        CardResponse response =
                new CardResponse();
        response.setId(20L);

        when(cardMapper.toResponse(card))
                .thenReturn(response);

        CardResponse result =
                cardService.getCard(20L, email);

        assertEquals(20L, result.getId());

        verify(cardRepository)
                .findByIdAndBankAccountUserId(
                        20L,
                        1L
                );
    }

    @Test
    void getCardShouldThrowWhenCardDoesNotBelongToCurrentUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(cardRepository.findByIdAndBankAccountUserId(
                99L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> cardService.getCard(
                        99L,
                        email
                )
        );

        verify(cardMapper, never())
                .toResponse(any(Card.class));
    }
}