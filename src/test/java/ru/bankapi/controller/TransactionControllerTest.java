package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.TransactionService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void depositShouldReturnOk() throws Exception {
        TransactionResponse response =
                new TransactionResponse();

        response.setId(20L);
        response.setType(TransactionType.DEPOSIT);
        response.setDestinationAccountId(10L);
        response.setAmount(new BigDecimal("500.00"));
        response.setDescription("Пополнение");

        when(transactionService.deposit(
                eq(10L),
                eq("ivan@example.com"),
                any(MoneyOperationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/accounts/10/deposit")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "amount": 500.00,
                                          "description": "Пополнение"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").value(20)
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("DEPOSIT")
                )
                .andExpect(
                        jsonPath("$.destinationAccountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.sourceAccountId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(500.00)
                );

        verify(transactionService)
                .deposit(
                        eq(10L),
                        eq("ivan@example.com"),
                        any(MoneyOperationRequest.class)
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void withdrawShouldReturnOk() throws Exception {
        TransactionResponse response =
                new TransactionResponse();

        response.setId(21L);
        response.setType(TransactionType.WITHDRAWAL);
        response.setSourceAccountId(10L);
        response.setAmount(new BigDecimal("250.00"));

        when(transactionService.withdraw(
                eq(10L),
                eq("ivan@example.com"),
                any(MoneyOperationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/accounts/10/withdraw")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "amount": 250.00,
                                          "description": "Снятие"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.type")
                                .value("WITHDRAWAL")
                )
                .andExpect(
                        jsonPath("$.sourceAccountId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.destinationAccountId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(250.00)
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void depositShouldReturnBadRequestWhenAmountIsZero()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts/10/deposit")
                                .contentType("application/json")
                                .content("""
                                        {
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
                                .value("Сумма должна быть больше нуля")
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void withdrawShouldReturnBadRequestWhenAmountIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts/10/withdraw")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "description": "Снятие"
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
    void withdrawShouldReturnConflictWhenBalanceIsInsufficient()
            throws Exception {

        when(transactionService.withdraw(
                eq(10L),
                eq("ivan@example.com"),
                any(MoneyOperationRequest.class)
        )).thenThrow(
                new InvalidOperationException(
                        "Недостаточно средств"
                )
        );

        mockMvc.perform(
                        post("/api/accounts/10/withdraw")
                                .contentType("application/json")
                                .content("""
                                        {
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
}