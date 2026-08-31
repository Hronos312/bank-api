package ru.bankapi.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.transaction.TransactionPageResponse;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.BadRequestException;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.admin.AdminTransactionService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminTransactionService
            adminTransactionService;

    @MockitoBean
    private JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionsShouldReturnOk()
            throws Exception {

        TransactionResponse transaction =
                new TransactionResponse();

        transaction.setId(10L);
        transaction.setType(
                TransactionType.TRANSFER
        );
        transaction.setAmount(
                new BigDecimal("500.00")
        );

        TransactionPageResponse response =
                new TransactionPageResponse();

        response.setContent(
                List.of(transaction)
        );
        response.setPage(0);
        response.setSize(20);
        response.setTotalElements(1);
        response.setTotalPages(1);
        response.setFirst(true);
        response.setLast(true);

        when(
                adminTransactionService
                        .getTransactions(
                                0,
                                20
                        )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/admin/transactions"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.content[0].type")
                                .value("TRANSFER")
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                );

        verify(adminTransactionService)
                .getTransactions(
                        0,
                        20
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionsShouldUseProvidedPagination()
            throws Exception {

        TransactionPageResponse response =
                new TransactionPageResponse();

        response.setContent(List.of());
        response.setPage(3);
        response.setSize(50);
        response.setTotalElements(0);
        response.setTotalPages(0);
        response.setFirst(false);
        response.setLast(true);

        when(
                adminTransactionService
                        .getTransactions(
                                3,
                                50
                        )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/admin/transactions"
                        )
                                .param(
                                        "page",
                                        "3"
                                )
                                .param(
                                        "size",
                                        "50"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.page")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(50)
                );

        verify(adminTransactionService)
                .getTransactions(
                        3,
                        50
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionShouldReturnOk()
            throws Exception {

        TransactionResponse response =
                new TransactionResponse();

        response.setId(10L);
        response.setType(
                TransactionType.WITHDRAWAL
        );
        response.setAmount(
                new BigDecimal("250.00")
        );

        when(
                adminTransactionService
                        .getTransaction(10L)
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/admin/transactions/10"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("WITHDRAWAL")
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(250.00)
                );

        verify(adminTransactionService)
                .getTransaction(10L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionShouldReturnNotFound()
            throws Exception {

        when(
                adminTransactionService
                        .getTransaction(99L)
        ).thenThrow(
                new NotFoundException(
                        "Транзакция не найдена"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/admin/transactions/99"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Транзакция не найдена"
                                )
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionsShouldReturnBadRequestWhenPageIsNegative()
            throws Exception {

        when(
                adminTransactionService
                        .getTransactions(
                                -1,
                                20
                        )
        ).thenThrow(
                new BadRequestException(
                        "Номер страницы не может быть отрицательным"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/admin/transactions"
                        )
                                .param(
                                        "page",
                                        "-1"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("BAD_REQUEST")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getTransactionsShouldReturnBadRequestWhenSizeIsTooLarge()
            throws Exception {

        when(
                adminTransactionService
                        .getTransactions(
                                0,
                                101
                        )
        ).thenThrow(
                new BadRequestException(
                        "Размер страницы должен быть от 1 до 100"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/admin/transactions"
                        )
                                .param(
                                        "size",
                                        "101"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("BAD_REQUEST")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Размер страницы должен быть от 1 до 100"
                                )
                );
    }
}