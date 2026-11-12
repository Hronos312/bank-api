package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createUserShouldCreateClient() {
        UserCreationService userCreationService =
                new UserCreationService(
                        userRepository,
                        passwordEncoder
                );

        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        User result = userCreationService.createUser(
                request,
                UserRole.CLIENT
        );

        assertEquals(
                request.getEmail(),
                result.getEmail()
        );

        assertEquals(
                "encoded-password",
                result.getPasswordHash()
        );

        assertEquals(
                UserRole.CLIENT,
                result.getRole()
        );

        assertEquals(
                UserStatus.ACTIVE,
                result.getStatus()
        );

        verify(passwordEncoder)
                .encode(request.getPassword());

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void createUserShouldUseProvidedRole() {
        UserCreationService userCreationService =
                new UserCreationService(
                        userRepository,
                        passwordEncoder
                );

        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        User result = userCreationService.createUser(
                request,
                UserRole.ADMIN
        );

        assertEquals(
                UserRole.ADMIN,
                result.getRole()
        );

        assertEquals(
                UserStatus.ACTIVE,
                result.getStatus()
        );
    }

    @Test
    void createUserShouldThrowWhenEmailAlreadyExists() {
        UserCreationService userCreationService =
                new UserCreationService(
                        userRepository,
                        passwordEncoder
                );

        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        assertThrows(
                DuplicateDataException.class,
                () -> userCreationService.createUser(
                        request,
                        UserRole.CLIENT
                )
        );

        verify(userRepository, never())
                .existsByPhone(anyString());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void createUserShouldThrowWhenPhoneAlreadyExists() {
        UserCreationService userCreationService =
                new UserCreationService(
                        userRepository,
                        passwordEncoder
                );

        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(true);

        assertThrows(
                DuplicateDataException.class,
                () -> userCreationService.createUser(
                        request,
                        UserRole.CLIENT
                )
        );

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void createUserShouldFillUserFields() {
        UserCreationService userCreationService =
                new UserCreationService(
                        userRepository,
                        passwordEncoder
                );

        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByPhone(request.getPhone()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        userCreationService.createUser(
                request,
                UserRole.CLIENT
        );

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(
                request.getFirstName(),
                savedUser.getFirstName()
        );

        assertEquals(
                request.getLastName(),
                savedUser.getLastName()
        );

        assertEquals(
                request.getMiddleName(),
                savedUser.getMiddleName()
        );

        assertEquals(
                request.getBirthDate(),
                savedUser.getBirthDate()
        );

        assertEquals(
                request.getPhone(),
                savedUser.getPhone()
        );
    }

    private RegisterRequest createRequest() {
        RegisterRequest request =
                new RegisterRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("password123");
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setMiddleName("Ivanovich");

        request.setBirthDate(
                LocalDate.of(2000, 1, 1)
        );

        request.setPhone("+79990000001");

        return request;
    }
}