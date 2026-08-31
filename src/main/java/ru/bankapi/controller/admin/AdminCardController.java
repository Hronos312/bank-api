package ru.bankapi.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.service.admin.AdminCardService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin - Cards", description = "Администрирование банковских карт")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {

    private final AdminCardService adminCardService;

    @GetMapping
    @Operation(summary = "Получить все карты",  description = "Возвращает карты всех пользователей системы")
    @ApiResponse(responseCode = "200", description = "Список карт получен")
    public List<CardResponse> getCards() {
        return adminCardService.getCards();
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить карту по id", description = "Возвращает карту по её id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта найдена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    public CardResponse getCard(@PathVariable Long cardId) {
        return adminCardService.getCard(cardId);
    }

    @PatchMapping("/{cardId}/block")
    @Operation(summary = "Заблокировать карту", description = "Переводит активную карту в статус BLOCKED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта заблокирована"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "409", description = "Изменение статуса невозможно")
    })
    public CardResponse blockCard(@PathVariable Long cardId) {
        return adminCardService.blockCard(cardId);
    }

    @PatchMapping("/{cardId}/unblock")
    @Operation(summary = "Разблокировать карту", description = "Переводит карту из BLOCKED в ACTIVE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта разблокирована"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "409", description = "Карту нельзя разблокировать в её текущем состоянии")
    })
    public CardResponse unblockCard(@PathVariable Long cardId) {
        return adminCardService.unblockCard(cardId);
    }
}