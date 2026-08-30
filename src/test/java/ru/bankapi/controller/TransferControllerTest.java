package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.TransferService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnOk() throws Exception {
        TransactionResponse response =
                new TransactionResponse();

        response.setId(30L);
        response.setType(TransactionType.TRANSFER);
        response.setSourceAccountId(10L);
        response.setDestinationAccountId(20L);
        response.setAmount(new BigDecimal("400.00"));
        response.setDescription("Перевод");

        when(transferService.transfer(
                eq(10L),
                eq("ivan@example.com"),
                any(TransferRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "destinationAccountNumber":
                                            "40817810000000000002",
                                          "amount": 400.00,
                                          "description": "Перевод"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(30)
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("TRANSFER")
                )
                .andExpect(
                        jsonPath("$.sourceAccountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.destinationAccountId")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(400.00)
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Перевод")
                );

        verify(transferService)
                .transfer(
                        eq(10L),
                        eq("ivan@example.com"),
                        any(TransferRequest.class)
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnBadRequestWhenAmountIsZero()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "destinationAccountNumber":
                                            "40817810000000000002",
                                          "amount": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Сумма должна быть больше нуля"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnBadRequestWhenAmountIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "destinationAccountNumber":
                                            "40817810000000000002"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Сумма обязательна")
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnBadRequestWhenDestinationAccountNumberIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "amount": 100.00
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Номер счёта получателя обязателен"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnConflictWhenBalanceIsInsufficient()
            throws Exception {

        when(transferService.transfer(
                eq(10L),
                eq("ivan@example.com"),
                any(TransferRequest.class)
        )).thenThrow(
                new InvalidOperationException(
                        "Недостаточно средств"
                )
        );

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "destinationAccountNumber":
                                            "40817810000000000002",
                                          "amount": 5000.00
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_OPERATION")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Недостаточно средств")
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void transferShouldReturnNotFoundWhenDestinationAccountDoesNotExist()
            throws Exception {

        when(transferService.transfer(
                eq(10L),
                eq("ivan@example.com"),
                any(TransferRequest.class)
        )).thenThrow(
                new NotFoundException(
                        "Счёт получателя не найден"
                )
        );

        mockMvc.perform(
                        post("/api/accounts/10/transfer")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "destinationAccountNumber":
                                            "40817810000000000099",
                                          "amount": 100.00
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Счёт получателя не найден"
                                )
                );
    }
}