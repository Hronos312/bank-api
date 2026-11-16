package ru.bankapi.fixture;

import lombok.Getter;
import lombok.Setter;
import ru.bankapi.enums.UserStatus;

@Getter
@Setter
public class TestUser {

    private Long id;
    private String email;
    private UserStatus status = UserStatus.ACTIVE;
}
