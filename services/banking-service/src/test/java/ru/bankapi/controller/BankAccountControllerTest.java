package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CurrencyCode;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.BankAccountService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankAccountService bankAccountService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void createAccountShouldReturnCreated() throws Exception {
        AccountResponse response = createAccountResponse();

        when(bankAccountService.createAccount("ivan@example.com"))
                .thenReturn(response);

        mockMvc.perform(post("/api/accounts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value("40817810000000000001")
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("RUB")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(bankAccountService)
                .createAccount("ivan@example.com");
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getAccountsShouldReturnCurrentUserAccounts()
            throws Exception {

        AccountResponse first = createAccountResponse();

        AccountResponse second = new AccountResponse();
        second.setId(11L);
        second.setAccountNumber("40817810000000000002");
        second.setBalance(new BigDecimal("1500.00"));
        second.setCurrency(CurrencyCode.RUB);
        second.setStatus(AccountStatus.ACTIVE);

        when(bankAccountService.getAccounts("ivan@example.com"))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(
                        jsonPath("$[0].accountNumber")
                                .value("40817810000000000001")
                )
                .andExpect(
                        jsonPath("$[1].accountNumber")
                                .value("40817810000000000002")
                );

        verify(bankAccountService)
                .getAccounts("ivan@example.com");
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getAccountShouldReturnCurrentUserAccount()
            throws Exception {

        AccountResponse response = createAccountResponse();

        when(bankAccountService.getAccount(
                10L,
                "ivan@example.com"
        )).thenReturn(response);

        mockMvc.perform(get("/api/accounts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value("40817810000000000001")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("RUB")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(bankAccountService)
                .getAccount(
                        10L,
                        "ivan@example.com"
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getAccountShouldReturnNotFoundWhenAccountDoesNotExist()
            throws Exception {

        when(bankAccountService.getAccount(
                99L,
                "ivan@example.com"
        )).thenThrow(
                new NotFoundException("Счёт не найден")
        );

        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Счёт не найден")
                );

        verify(bankAccountService)
                .getAccount(
                        99L,
                        "ivan@example.com"
                );
    }

    private AccountResponse createAccountResponse() {
        AccountResponse response = new AccountResponse();

        response.setId(10L);
        response.setAccountNumber("40817810000000000001");
        response.setBalance(BigDecimal.ZERO);
        response.setCurrency(CurrencyCode.RUB);
        response.setStatus(AccountStatus.ACTIVE);

        return response;
    }
}