package ru.bankapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.card.CardResponse;
import ru.bankapi.service.CardService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Банковские карты пользователя")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @PostMapping("/accounts/{accountId}/card")
    @Operation(summary = "Выпустить карту", description = "Выпускает карту для банковского счёта текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Карта успешно выпущена"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден"),
            @ApiResponse(responseCode = "409", description = "Для счёта уже выпущена карта или операция недоступна")
    })
    public ResponseEntity<CardResponse> issueCard(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CardResponse response = cardService.issueCard(accountId, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/cards")
    @Operation(summary = "Получить карты", description = "Возвращает все карты текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Список карт получен")
    public List<CardResponse> getCards(@AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCards(userDetails.getUsername());
    }

    @GetMapping("/cards/{cardId}")
    @Operation(summary = "Получить карту", description = "Возвращает карту текущего пользователя по её id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта найдена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    public CardResponse getCard(@PathVariable Long cardId, @AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCard(cardId, userDetails.getUsername());
    }
}