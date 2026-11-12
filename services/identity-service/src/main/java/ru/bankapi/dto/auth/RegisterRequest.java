package ru.bankapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Данные для регистрации пользователя")
public class RegisterRequest {

    @NotBlank(message = "Email не должен быть пустым")
    @Email(message = "Некорректный формат email")
    @Schema(example = "ivan@example.com")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    @Schema(example = "password123")
    private String password;

    @NotBlank(message = "Имя не должно быть пустым")
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    @Schema(example = "Иван")
    private String firstName;

    @NotBlank(message = "Фамилия не должна быть пустой")
    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    @Schema(example = "Иванов")
    private String lastName;

    @Size(max = 100, message = "Отчество не должно превышать 100 символов")
    @Schema(example = "Иванович")
    private String middleName;

    @NotNull(message = "Дата рождения обязательна")
    @Past(message = "Дата рождения должна быть в прошлом")
    @Schema(example = "2000-01-01", type = "string", format = "date")
    private LocalDate birthDate;

    @NotBlank(message = "Телефон не должен быть пустым")
    @Size(max = 20, message = "Номер телефона слишком длинный")
    @Schema(example = "+79990000001")
    private String phone;
}