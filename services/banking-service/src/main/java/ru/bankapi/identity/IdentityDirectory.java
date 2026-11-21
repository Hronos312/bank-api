package ru.bankapi.identity;

import ru.bankapi.enums.UserStatus;

import java.util.Optional;

public interface IdentityDirectory {

    Optional<UserIdentity> findBySubject(String subject);

    record UserIdentity(Long id, UserStatus status) {
    }
}
