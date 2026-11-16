package ru.bankapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.model.User;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String subject) {
        User user = userRepository.findById(Long.parseLong(subject))
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .roles(user.getRole().name())
                .build();
    }
}
