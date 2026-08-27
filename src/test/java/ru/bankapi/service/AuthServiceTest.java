package ru.bankapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.exception.InvalidCredentialsException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;
import ru.bankapi.security.JwtService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("password123");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setMiddleName("Иванович");
        request.setBirthDate(LocalDate.of(2000, 5, 15));
        request.setPhone("+79991234567");
    }

    @Test
    void registerShouldCreateUser() {
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail(request.getEmail());

        when(userMapper.toResponse(any(User.class)))
                .thenReturn(expectedResponse);

        UserResponse result = authService.register(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(request.getEmail(), result.getEmail());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(request.getEmail(), savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());

        assertEquals(request.getFirstName(), savedUser.getFirstName());
        assertEquals(request.getLastName(), savedUser.getLastName());
        assertEquals(request.getMiddleName(), savedUser.getMiddleName());

        assertEquals(request.getBirthDate(), savedUser.getBirthDate());
        assertEquals(request.getPhone(), savedUser.getPhone());

        assertEquals(UserRole.CLIENT, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByPhone(request.getPhone());

        verify(passwordEncoder).encode(request.getPassword());

        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    void registerShouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        DuplicateDataException exception = assertThrows(
                DuplicateDataException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "EMAIL_ALREADY_EXISTS",
                exception.getCode()
        );

        verify(userRepository).existsByEmail(request.getEmail());

        verify(userRepository, never())
                .existsByPhone(anyString());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));

        verify(userMapper, never())
                .toResponse(any(User.class));
    }

    @Test
    void registerShouldThrowExceptionWhenPhoneAlreadyExists() {
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(true);

        DuplicateDataException exception = assertThrows(
                DuplicateDataException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "PHONE_ALREADY_EXISTS",
                exception.getCode()
        );

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByPhone(request.getPhone());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));

        verify(userMapper, never())
                .toResponse(any(User.class));
    }

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("ivan@example.com");
        loginRequest.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("ivan@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPasswordHash()
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());

        verify(userRepository).findByEmail(loginRequest.getEmail());

        verify(passwordEncoder).matches(
                loginRequest.getPassword(),
                user.getPasswordHash()
        );

        verify(jwtService).generateToken(user);
    }

    @Test
    void loginShouldThrowExceptionWhenUserDoesNotExist() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unknown@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(userRepository).findByEmail(loginRequest.getEmail());

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    @Test
    void loginShouldThrowExceptionWhenPasswordIsIncorrect() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("ivan@example.com");
        loginRequest.setPassword("wrong-password");

        User user = new User();
        user.setEmail("ivan@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPasswordHash()
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(passwordEncoder).matches(
                loginRequest.getPassword(),
                user.getPasswordHash()
        );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }
}