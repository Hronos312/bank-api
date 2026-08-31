package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.AuthResponse;
import ru.bankapi.dto.auth.LoginRequest;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.AccountBlockedException;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.exception.InvalidCredentialsException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;
import ru.bankapi.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserCreationService userCreationService;

    public UserResponse register(RegisterRequest request) {
        User user = userCreationService.createUser(request, UserRole.CLIENT);

        return userMapper.toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Неверный email или пароль");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Учётная запись заблокирована");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }
}