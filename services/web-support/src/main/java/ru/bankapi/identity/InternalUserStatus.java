package ru.bankapi.identity;

import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;

public record InternalUserStatus(Long id, UserStatus status, UserRole role) {
}
