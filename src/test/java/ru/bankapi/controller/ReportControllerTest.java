package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getSpendingReportShouldReturnOk()
            throws Exception {

        SpendingReportResponse response =
                new SpendingReportResponse();

        response.setWithdrawals(
                new BigDecimal("750.50")
        );

        response.setOutgoingTransfers(
                new BigDecimal("1000.00")
        );

        response.setTotalSpent(
                new BigDecimal("1750.50")
        );

        response.setGeneratedAt(
                LocalDateTime.of(
                        2026,
                        8,
                        31,
                        14,
                        30
                )
        );

        when(reportService.getSpendingReport(
                "ivan@example.com"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/reports/spending")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.withdrawals")
                                .value(750.50)
                )
                .andExpect(
                        jsonPath("$.outgoingTransfers")
                                .value(1000.00)
                )
                .andExpect(
                        jsonPath("$.totalSpent")
                                .value(1750.50)
                )
                .andExpect(
                        jsonPath("$.generatedAt")
                                .value(
                                        "2026-08-31T14:30:00"
                                )
                );

        verify(reportService)
                .getSpendingReport(
                        "ivan@example.com"
                );
    }

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getSpendingReportShouldReturnZeroValues()
            throws Exception {

        SpendingReportResponse response =
                new SpendingReportResponse();

        response.setWithdrawals(BigDecimal.ZERO);
        response.setOutgoingTransfers(BigDecimal.ZERO);
        response.setTotalSpent(BigDecimal.ZERO);

        when(reportService.getSpendingReport(
                "ivan@example.com"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/reports/spending")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.withdrawals")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.outgoingTransfers")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.totalSpent")
                                .value(0)
                );
    }

    @Test
    @WithMockUser(
            username = "missing@example.com",
            roles = "CLIENT"
    )
    void getSpendingReportShouldReturnNotFoundWhenUserDoesNotExist()
            throws Exception {

        when(reportService.getSpendingReport(
                "missing@example.com"
        )).thenThrow(
                new NotFoundException(
                        "Пользователь не найден"
                )
        );

        mockMvc.perform(
                        get("/api/reports/spending")
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Пользователь не найден"
                                )
                );

        verify(reportService)
                .getSpendingReport(
                        "missing@example.com"
                );
    }
}