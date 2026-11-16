package ru.bankapi.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.enums.UserRole;
import ru.bankapi.model.User;
import ru.bankapi.service.UserCreationService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCreationService userCreationService;

    @Mock
    private ApplicationArguments applicationArguments;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(
                userRepository,
                userCreationService
        );
    }

    @Test
    void runShouldDoNothingWhenBootstrapVariablesAreNotConfigured() {
        setConfiguration(
                "",
                "",
                "",
                "System",
                "Administrator",
                "1970-01-01"
        );

        adminInitializer.run(applicationArguments);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userCreationService);
    }

    @Test
    void runShouldCreateAdminWhenAdminDoesNotExist() {
        setConfiguration(
                "admin@bank.local",
                "AdminPassword123",
                "+70000000001",
                "Ivan",
                "Ivanov",
                "2000-01-01"
        );

        when(
                userRepository.findByEmail(
                        "admin@bank.local"
                )
        ).thenReturn(Optional.empty());

        adminInitializer.run(applicationArguments);

        ArgumentCaptor<RegisterRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        RegisterRequest.class
                );

        verify(userCreationService)
                .createUser(
                        requestCaptor.capture(),
                        eq(UserRole.ADMIN)
                );

        RegisterRequest request =
                requestCaptor.getValue();

        assertEquals(
                "admin@bank.local",
                request.getEmail()
        );

        assertEquals(
                "AdminPassword123",
                request.getPassword()
        );

        assertEquals(
                "+70000000001",
                request.getPhone()
        );

        assertEquals(
                "Ivan",
                request.getFirstName()
        );

        assertEquals(
                "Ivanov",
                request.getLastName()
        );

        assertEquals(
                LocalDate.of(2000, 1, 1),
                request.getBirthDate()
        );
    }

    @Test
    void runShouldDoNothingWhenAdminAlreadyExists() {
        setConfiguration(
                "admin@bank.local",
                "AdminPassword123",
                "+70000000001",
                "System",
                "Administrator",
                "1970-01-01"
        );

        User existingAdmin = new User();

        existingAdmin.setEmail(
                "admin@bank.local"
        );

        existingAdmin.setRole(
                UserRole.ADMIN
        );

        when(
                userRepository.findByEmail(
                        "admin@bank.local"
                )
        ).thenReturn(
                Optional.of(existingAdmin)
        );

        adminInitializer.run(applicationArguments);

        verify(
                userCreationService,
                never()
        ).createUser(
                any(RegisterRequest.class),
                any(UserRole.class)
        );
    }

    @Test
    void runShouldThrowWhenExistingUserIsNotAdmin() {
        setConfiguration(
                "admin@bank.local",
                "AdminPassword123",
                "+70000000001",
                "System",
                "Administrator",
                "1970-01-01"
        );

        User existingClient = new User();

        existingClient.setEmail(
                "admin@bank.local"
        );

        existingClient.setRole(
                UserRole.CLIENT
        );

        when(
                userRepository.findByEmail(
                        "admin@bank.local"
                )
        ).thenReturn(
                Optional.of(existingClient)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminInitializer.run(
                                applicationArguments
                        )
                );

        assertEquals(
                "Пользователь с ADMIN_EMAIL уже существует, "
                        + "но не является администратором",
                exception.getMessage()
        );

        verify(
                userCreationService,
                never()
        ).createUser(
                any(RegisterRequest.class),
                any(UserRole.class)
        );
    }

    @Test
    void runShouldThrowWhenEmailIsMissing() {
        setConfiguration(
                "",
                "AdminPassword123",
                "+70000000001",
                "System",
                "Administrator",
                "1970-01-01"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminInitializer.run(
                                applicationArguments
                        )
                );

        assertEquals(
                "Не задана переменная ADMIN_EMAIL",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userCreationService);
    }

    @Test
    void runShouldThrowWhenPasswordIsMissing() {
        setConfiguration(
                "admin@bank.local",
                "",
                "+70000000001",
                "System",
                "Administrator",
                "1970-01-01"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminInitializer.run(
                                applicationArguments
                        )
                );

        assertEquals(
                "Не задана переменная ADMIN_PASSWORD",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userCreationService);
    }

    @Test
    void runShouldThrowWhenPhoneIsMissing() {
        setConfiguration(
                "admin@bank.local",
                "AdminPassword123",
                "",
                "System",
                "Administrator",
                "1970-01-01"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> adminInitializer.run(
                                applicationArguments
                        )
                );

        assertEquals(
                "Не задана переменная ADMIN_PHONE",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userCreationService);
    }

    private void setConfiguration(
            String email,
            String password,
            String phone,
            String firstName,
            String lastName,
            String birthDate
    ) {
        ReflectionTestUtils.setField(
                adminInitializer,
                "email",
                email
        );

        ReflectionTestUtils.setField(
                adminInitializer,
                "password",
                password
        );

        ReflectionTestUtils.setField(
                adminInitializer,
                "phone",
                phone
        );

        ReflectionTestUtils.setField(
                adminInitializer,
                "firstName",
                firstName
        );

        ReflectionTestUtils.setField(
                adminInitializer,
                "lastName",
                lastName
        );

        ReflectionTestUtils.setField(
                adminInitializer,
                "birthDate",
                birthDate
        );
    }
}