package ru.bankapi.dto.user;

import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate birthDate;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;

}