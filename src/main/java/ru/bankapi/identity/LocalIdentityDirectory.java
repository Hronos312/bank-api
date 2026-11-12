package ru.bankapi.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.bankapi.dal.UserRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class LocalIdentityDirectory implements IdentityDirectory {

    private final UserRepository userRepository;

    @Override
    public Optional<UserIdentity> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(user -> new UserIdentity(user.getId(), user.getStatus()));
    }
}
