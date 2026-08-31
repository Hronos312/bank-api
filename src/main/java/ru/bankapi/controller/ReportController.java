package ru.bankapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bankapi.dto.report.SpendingReportResponse;
import ru.bankapi.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Финансовые отчёты пользователя")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/spending")
    @Operation(summary = "Получить отчёт о расходах", description = "Возвращает общие расходы пользователя за всё время: снятия и переводы другим пользователям")
    @ApiResponse(responseCode = "200", description = "Отчёт сформирован")
    public SpendingReportResponse getSpendingReport(@AuthenticationPrincipal UserDetails userDetails) {
        return reportService.getSpendingReport(userDetails.getUsername());
    }
}