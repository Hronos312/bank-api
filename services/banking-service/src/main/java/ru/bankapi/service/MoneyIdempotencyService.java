package ru.bankapi.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.BadRequestException;
import ru.bankapi.exception.DuplicateDataException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyIdempotencyService {

    public static final String HEADER = "Idempotency-Key";
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    @Transactional
    public TransactionResponse execute(long userId, String key, Operation operation,
                                       Supplier<TransactionResponse> perform) {
        if (!key.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new BadRequestException("Недопустимый формат ключа идемпотентности");
        }
        String fingerprint = fingerprint(operation);
        int inserted = jdbc.sql("""
                INSERT INTO money_requests (user_id, request_key, fingerprint)
                VALUES (?, ?, ?) ON CONFLICT (user_id, request_key) DO NOTHING
                """).params(userId, key, fingerprint).update();
        if (inserted == 0) {
            var saved = jdbc.sql("""
                    SELECT fingerprint, response::text AS response
                    FROM money_requests WHERE user_id = ? AND request_key = ?
                    """).params(userId, key)
                    .query((row, index) -> new SavedRequest(row.getString("fingerprint"), row.getString("response")))
                    .single();
            if (!saved.fingerprint().equals(fingerprint)) {
                throw new DuplicateDataException("IDEMPOTENCY_CONFLICT",
                        "Этот ключ идемпотентности уже использован для другой операции");
            }
            log.debug("Возвращён сохранённый результат денежной операции");
            return readResponse(saved.response());
        }

        TransactionResponse response = perform.get();
        jdbc.sql("""
                UPDATE money_requests SET response = CAST(? AS jsonb)
                WHERE user_id = ? AND request_key = ?
                """).params(writeJson(response), userId, key).update();
        return response;
    }

    private String fingerprint(Operation operation) {
        try {
            byte[] bytes = writeJson(operation).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Недоступен алгоритм SHA-256", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Не удалось сохранить данные идемпотентной операции", exception);
        }
    }

    private TransactionResponse readResponse(String value) {
        try {
            return json.readValue(value, TransactionResponse.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Не удалось прочитать сохранённый результат операции", exception);
        }
    }

    public record Operation(TransactionType type, long accountId, BigDecimal amount,
                            String description, String destinationAccountNumber) {
        public Operation {
            amount = amount.stripTrailingZeros();
        }
    }

    private record SavedRequest(String fingerprint, String response) {
    }
}
