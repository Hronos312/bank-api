package ru.bankapi.identity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.bankapi.enums.UserStatus;

import java.util.List;

public record BankingPrincipal(InternalUserStatus identity) implements UserDetails {

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + identity.role()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return identity.id().toString();
    }

    @Override
    public boolean isEnabled() {
        return identity.status() == UserStatus.ACTIVE;
    }
}
