package ru.bankapi.dto.card;

import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.CardStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CardResponse {

    private Long id;
    private String cardNumber;
    private Long accountId;
    private LocalDate expirationDate;
    private CardStatus status;
    private LocalDateTime createdAt;

}