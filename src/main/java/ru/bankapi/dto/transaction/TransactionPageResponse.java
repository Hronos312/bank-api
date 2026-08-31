package ru.bankapi.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Страница банковских транзакций")
public class TransactionPageResponse {

    @Schema(description = "Транзакции текущей страницы")
    private List<TransactionResponse> content;

    @Schema(description = "Номер текущей страницы, начиная с 0", example = "0")
    private int page;

    @Schema(description = "Количество элементов на странице", example = "20")
    private int size;

    @Schema(description = "Общее количество транзакций", example = "123")
    private long totalElements;

    @Schema(description = "Общее количество страниц", example = "7")
    private int totalPages;

    @Schema(description = "Является ли страница первой", example = "true")
    private boolean first;

    @Schema(description = "Является ли страница последней", example = "false")
    private boolean last;
}