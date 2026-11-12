package ru.bankapi.controller.admin;

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
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.admin.AdminAccountService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccountService adminAccountService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getAccountsShouldReturnOk()
            throws Exception {

        AccountResponse first =
                createResponse(
                        1L,
                        "100.00",
                        AccountStatus.ACTIVE
                );

        AccountResponse second =
                createResponse(
                        2L,
                        "0.00",
                        AccountStatus.BLOCKED
                );

        when(adminAccountService.getAccounts())
                .thenReturn(List.of(first, second));

        mockMvc.perform(
                        get("/api/admin/accounts")
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
    void getAccountShouldReturnOk()
            throws Exception {

        AccountResponse response =
                createResponse(
                        1L,
                        "100.00",
                        AccountStatus.ACTIVE
                );

        when(adminAccountService.getAccount(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/admin/accounts/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(100.00)
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
    void blockAccountShouldReturnBlockedAccount()
            throws Exception {

        AccountResponse response =
                createResponse(
                        1L,
                        "100.00",
                        AccountStatus.BLOCKED
                );

        when(adminAccountService.blockAccount(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/admin/accounts/1/block")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        verify(adminAccountService)
                .blockAccount(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void unblockAccountShouldReturnActiveAccount()
            throws Exception {

        AccountResponse response =
                createResponse(
                        1L,
                        "100.00",
                        AccountStatus.ACTIVE
                );

        when(adminAccountService.unblockAccount(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/admin/accounts/1/unblock")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(adminAccountService)
                .unblockAccount(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void closeAccountShouldReturnClosedAccount()
            throws Exception {

        AccountResponse response =
                createResponse(
                        1L,
                        "0.00",
                        AccountStatus.CLOSED
                );

        when(adminAccountService.closeAccount(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/admin/accounts/1/close")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CLOSED")
                );

        verify(adminAccountService)
                .closeAccount(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getAccountShouldReturnNotFound()
            throws Exception {

        when(adminAccountService.getAccount(99L))
                .thenThrow(
                        new NotFoundException(
                                "Счёт не найден"
                        )
                );

        mockMvc.perform(
                        get("/api/admin/accounts/99")
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Счёт не найден")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void closeAccountShouldReturnConflictWhenBalanceIsNotZero()
            throws Exception {

        when(adminAccountService.closeAccount(1L))
                .thenThrow(
                        new InvalidOperationException(
                                "Нельзя закрыть счёт с ненулевым балансом"
                        )
                );

        mockMvc.perform(
                        patch("/api/admin/accounts/1/close")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_OPERATION")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Нельзя закрыть счёт с ненулевым балансом"
                                )
                );
    }

    private AccountResponse createResponse(
            Long id,
            String balance,
            AccountStatus status
    ) {
        AccountResponse response =
                new AccountResponse();

        response.setId(id);
        response.setBalance(
                new BigDecimal(balance)
        );
        response.setStatus(status);

        return response;
    }
}