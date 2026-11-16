package ru.bankapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Данные для входа в систему")
public class LoginRequest {

    @NotBlank(message = "Email не должен быть пустым")
    @Email(message = "Некорректный формат email")
    @Schema(description = "Email пользователя", example = "ivan@example.com")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    @Schema(description = "Пароль пользователя", example = "password123")
    private String password;

}