package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.CardService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void issueCardShouldReturnCreated() throws Exception {
        CardResponse response = createCardResponse();

        when(cardService.issueCard(
                10L,
                "ivan@example.com"
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/accounts/10/card")
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.cardNumber")
                                .value("2200123456789012")
                )
                .andExpect(
                        jsonPath("$.accountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.expirationDate")
                                .value("2029-08-30")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(cardService)
                .issueCard(
                        10L,
                        "ivan@example.com"
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getCardsShouldReturnCurrentUserCards()
            throws Exception {

        CardResponse first = createCardResponse();

        CardResponse second = new CardResponse();
        second.setId(21L);
        second.setCardNumber("2200987654321098");
        second.setAccountId(11L);
        second.setExpirationDate(
                LocalDate.of(2029, 8, 30)
        );
        second.setStatus(CardStatus.ACTIVE);

        when(cardService.getCards("ivan@example.com"))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(21)
                )
                .andExpect(
                        jsonPath("$[0].accountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$[1].accountId")
                                .value(11)
                );

        verify(cardService)
                .getCards("ivan@example.com");
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getCardShouldReturnCurrentUserCard()
            throws Exception {

        CardResponse response = createCardResponse();

        when(cardService.getCard(
                20L,
                "ivan@example.com"
        )).thenReturn(response);

        mockMvc.perform(get("/api/cards/20"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.cardNumber")
                                .value("2200123456789012")
                )
                .andExpect(
                        jsonPath("$.accountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(cardService)
                .getCard(
                        20L,
                        "ivan@example.com"
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getCardShouldReturnNotFoundWhenCardDoesNotExist()
            throws Exception {

        when(cardService.getCard(
                99L,
                "ivan@example.com"
        )).thenThrow(
                new NotFoundException("Карта не найдена")
        );

        mockMvc.perform(get("/api/cards/99"))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Карта не найдена")
                );

        verify(cardService)
                .getCard(
                        99L,
                        "ivan@example.com"
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void issueCardShouldReturnConflictWhenAccountAlreadyHasCard()
            throws Exception {

        when(cardService.issueCard(
                10L,
                "ivan@example.com"
        )).thenThrow(
                new InvalidOperationException(
                        "К этому счёту уже выпущена карта"
                )
        );

        mockMvc.perform(
                        post("/api/accounts/10/card")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_OPERATION")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "К этому счёту уже выпущена карта"
                                )
                );

        verify(cardService)
                .issueCard(
                        10L,
                        "ivan@example.com"
                );
    }

    private CardResponse createCardResponse() {
        CardResponse response = new CardResponse();

        response.setId(20L);
        response.setCardNumber("2200123456789012");
        response.setAccountId(10L);
        response.setExpirationDate(
                LocalDate.of(2029, 8, 30)
        );
        response.setStatus(CardStatus.ACTIVE);

        return response;
    }
}