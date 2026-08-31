package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.AuthResponse;
import ru.bankapi.dto.auth.LoginRequest;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.AccountBlockedException;
import ru.bankapi.exception.InvalidCredentialsException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;
import ru.bankapi.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserCreationService userCreationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldReturnCreatedUser() {
        RegisterRequest request =
                new RegisterRequest();

        request.setEmail("ivan@example.com");

        User user = new User();
        user.setId(1L);
        user.setEmail("ivan@example.com");
        user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE);

        UserResponse response =
                new UserResponse();

        response.setId(1L);
        response.setEmail("ivan@example.com");
        response.setRole(UserRole.CLIENT);
        response.setStatus(UserStatus.ACTIVE);

        when(
                userCreationService.createUser(
                        request,
                        UserRole.CLIENT
                )
        ).thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result =
                authService.register(request);

        assertEquals(1L, result.getId());

        assertEquals(
                "ivan@example.com",
                result.getEmail()
        );

        assertEquals(
                UserRole.CLIENT,
                result.getRole()
        );

        verify(userCreationService)
                .createUser(
                        request,
                        UserRole.CLIENT
                );

        verify(userMapper)
                .toResponse(user);
    }

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest request =
                new LoginRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        User user = new User();

        user.setEmail("ivan@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("ivan@example.com"))
                .thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        "password123",
                        "encoded-password"
                )
        ).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);

        AuthResponse result =
                authService.login(request);

        assertNotNull(result);

        verify(jwtService)
                .generateToken(user);
    }

    @Test
    void loginShouldThrowWhenUserDoesNotExist() {
        LoginRequest request =
                new LoginRequest();

        request.setEmail("missing@example.com");
        request.setPassword("password123");

        when(
                userRepository.findByEmail(
                        "missing@example.com"
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(passwordEncoder, never())
                .matches(
                        anyString(),
                        anyString()
                );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    @Test
    void loginShouldThrowWhenPasswordIsInvalid() {
        LoginRequest request =
                new LoginRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("wrong-password");

        User user = new User();

        user.setEmail("ivan@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("ivan@example.com"))
                .thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        "wrong-password",
                        "encoded-password"
                )
        ).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    @Test
    void loginShouldThrowWhenUserIsBlocked() {
        LoginRequest request =
                new LoginRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        User user = new User();

        user.setEmail("ivan@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findByEmail("ivan@example.com"))
                .thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        "password123",
                        "encoded-password"
                )
        ).thenReturn(true);

        assertThrows(
                AccountBlockedException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }
}