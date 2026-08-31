package ru.bankapi.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.CardStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Банковская карта")
public class CardResponse {

    @Schema(description = "Идентификатор карты", example = "1")
    private Long id;

    @Schema(description = "Номер банковской карты", example = "2200123456789012")
    private String cardNumber;

    @Schema(description = "Идентификатор счёта, к которому привязана карта", example = "1")
    private Long accountId;

    @Schema(description = "Дата окончания срока действия карты", example = "2031-08-31")
    private LocalDate expirationDate;

    @Schema(description = "Статус карты", example = "ACTIVE")
    private CardStatus status;

    @Schema(description = "Дата и время выпуска карты", example = "2026-08-31T18:00:00")
    private LocalDateTime createdAt;
}