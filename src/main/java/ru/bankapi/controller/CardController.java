package ru.bankapi.controller;

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
public class CardController {

    private final CardService cardService;

    @PostMapping("/accounts/{accountId}/card")
    public ResponseEntity<CardResponse> issueCard(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CardResponse response = cardService.issueCard(accountId, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/cards")
    public List<CardResponse> getCards(@AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCards(userDetails.getUsername());
    }

    @GetMapping("/cards/{cardId}")
    public CardResponse getCard(@PathVariable Long cardId, @AuthenticationPrincipal UserDetails userDetails) {
        return cardService.getCard(cardId, userDetails.getUsername());
    }
}