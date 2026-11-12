package ru.bankapi.controller.admin;

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
import ru.bankapi.service.admin.AdminCardService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCardService adminCardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getCardsShouldReturnOk()
            throws Exception {

        CardResponse first =
                createResponse(
                        1L,
                        CardStatus.ACTIVE
                );

        CardResponse second =
                createResponse(
                        2L,
                        CardStatus.BLOCKED
                );

        when(adminCardService.getCards())
                .thenReturn(List.of(first, second));

        mockMvc.perform(
                        get("/api/admin/cards")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(2)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getCardShouldReturnOk()
            throws Exception {

        CardResponse response =
                createResponse(
                        1L,
                        CardStatus.ACTIVE
                );

        when(adminCardService.getCard(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/admin/cards/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void blockCardShouldReturnBlockedCard()
            throws Exception {

        CardResponse response =
                createResponse(
                        1L,
                        CardStatus.BLOCKED
                );

        when(adminCardService.blockCard(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/admin/cards/1/block")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        verify(adminCardService)
                .blockCard(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void unblockCardShouldReturnActiveCard()
            throws Exception {

        CardResponse response =
                createResponse(
                        1L,
                        CardStatus.ACTIVE
                );

        when(adminCardService.unblockCard(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/admin/cards/1/unblock")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(adminCardService)
                .unblockCard(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getCardShouldReturnNotFound()
            throws Exception {

        when(adminCardService.getCard(99L))
                .thenThrow(
                        new NotFoundException(
                                "Карта не найдена"
                        )
                );

        mockMvc.perform(
                        get("/api/admin/cards/99")
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Карта не найдена")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void blockCardShouldReturnConflictWhenAlreadyBlocked()
            throws Exception {

        when(adminCardService.blockCard(1L))
                .thenThrow(
                        new InvalidOperationException(
                                "Карта уже заблокирована"
                        )
                );

        mockMvc.perform(
                        patch("/api/admin/cards/1/block")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_OPERATION")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Карта уже заблокирована")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void unblockCardShouldReturnConflictWhenCardIsClosed()
            throws Exception {

        when(adminCardService.unblockCard(1L))
                .thenThrow(
                        new InvalidOperationException(
                                "Закрытую карту нельзя разблокировать"
                        )
                );

        mockMvc.perform(
                        patch("/api/admin/cards/1/unblock")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_OPERATION")
                );
    }

    private CardResponse createResponse(
            Long id,
            CardStatus status
    ) {
        CardResponse response =
                new CardResponse();

        response.setId(id);
        response.setStatus(status);

        return response;
    }
}