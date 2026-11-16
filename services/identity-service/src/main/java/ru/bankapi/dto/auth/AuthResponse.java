package ru.bankapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Результат успешной аутентификации")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Тип токена", example = "Bearer")
    private String tokenType;

    @Schema(description = "Срок действия токена в секундах", example = "3600")
    private long expiresIn;

}