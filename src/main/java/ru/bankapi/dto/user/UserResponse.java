package ru.bankapi.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Пользователь банковской системы")
public class UserResponse {

    @Schema(description = "Идентификатор пользователя", example = "1")
    private Long id;

    @Schema(description = "Email пользователя", example = "ivan@example.com")
    private String email;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    private String lastName;

    @Schema(description = "Отчество", example = "Иванович")
    private String middleName;

    @Schema(description = "Дата рождения", example = "2000-01-01", type = "string", format = "date")
    private LocalDate birthDate;

    @Schema(description = "Номер телефона", example = "+79990000001")
    private String phone;

    @Schema(description = "Роль пользователя", example = "CLIENT")
    private UserRole role;

    @Schema(description = "Статус пользователя", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Дата и время создания пользователя", example = "2026-08-31T18:00:00")
    private LocalDateTime createdAt;
}