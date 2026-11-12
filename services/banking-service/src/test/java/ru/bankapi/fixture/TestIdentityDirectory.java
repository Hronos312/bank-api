package ru.bankapi.fixture;

import ru.bankapi.identity.IdentityDirectory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestIdentityDirectory implements IdentityDirectory {

    private final AtomicLong ids = new AtomicLong();
    private final Map<String, UserIdentity> users = new ConcurrentHashMap<>();

    public TestUser save(TestUser user) {
        user.setId(ids.incrementAndGet());
        users.put(user.getEmail(), new UserIdentity(user.getId(), user.getStatus()));
        return user;
    }

    public void deleteAll() {
        users.clear();
    }

    @Override
    public Optional<UserIdentity> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
