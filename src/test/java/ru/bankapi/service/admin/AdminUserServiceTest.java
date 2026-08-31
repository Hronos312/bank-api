package ru.bankapi.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;
import ru.bankapi.service.UserCreationService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserCreationService userCreationService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void getUsersShouldReturnAllUsers() {
        User first = createUser(
                1L,
                "first@example.com",
                UserStatus.ACTIVE
        );

        User second = createUser(
                2L,
                "second@example.com",
                UserStatus.BLOCKED
        );

        UserResponse firstResponse =
                new UserResponse();

        firstResponse.setId(1L);

        UserResponse secondResponse =
                new UserResponse();

        secondResponse.setId(2L);

        when(userRepository.findAll())
                .thenReturn(List.of(first, second));

        when(userMapper.toResponse(first))
                .thenReturn(firstResponse);

        when(userMapper.toResponse(second))
                .thenReturn(secondResponse);

        List<UserResponse> result =
                adminUserService.getUsers();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getUserShouldReturnUser() {
        User user = createUser(
                1L,
                "ivan@example.com",
                UserStatus.ACTIVE
        );

        UserResponse response =
                new UserResponse();

        response.setId(1L);
        response.setEmail("ivan@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result =
                adminUserService.getUser(1L);

        assertEquals(1L, result.getId());

        assertEquals(
                "ivan@example.com",
                result.getEmail()
        );
    }

    @Test
    void getUserShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> adminUserService.getUser(99L)
        );
    }

    @Test
    void createUserShouldCreateClient() {
        RegisterRequest request =
                new RegisterRequest();

        request.setEmail("new@example.com");

        User user = new User();

        user.setId(10L);
        user.setEmail("new@example.com");
        user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE);

        UserResponse response =
                new UserResponse();

        response.setId(10L);
        response.setEmail("new@example.com");
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
                adminUserService.createUser(request);

        assertEquals(10L, result.getId());

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
    void blockUserShouldChangeStatusToBlocked() {
        User user = createUser(
                1L,
                "ivan@example.com",
                UserStatus.ACTIVE
        );

        UserResponse response =
                new UserResponse();

        response.setId(1L);
        response.setStatus(UserStatus.BLOCKED);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result =
                adminUserService.blockUser(1L);

        assertEquals(
                UserStatus.BLOCKED,
                user.getStatus()
        );

        assertEquals(
                UserStatus.BLOCKED,
                result.getStatus()
        );
    }

    @Test
    void blockUserShouldThrowWhenUserAlreadyBlocked() {
        User user = createUser(
                1L,
                "ivan@example.com",
                UserStatus.BLOCKED
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidOperationException.class,
                () -> adminUserService.blockUser(1L)
        );

        verify(userMapper, never())
                .toResponse(any(User.class));
    }

    @Test
    void unblockUserShouldChangeStatusToActive() {
        User user = createUser(
                1L,
                "ivan@example.com",
                UserStatus.BLOCKED
        );

        UserResponse response =
                new UserResponse();

        response.setId(1L);
        response.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result =
                adminUserService.unblockUser(1L);

        assertEquals(
                UserStatus.ACTIVE,
                user.getStatus()
        );

        assertEquals(
                UserStatus.ACTIVE,
                result.getStatus()
        );
    }

    @Test
    void unblockUserShouldThrowWhenUserAlreadyActive() {
        User user = createUser(
                1L,
                "ivan@example.com",
                UserStatus.ACTIVE
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidOperationException.class,
                () -> adminUserService.unblockUser(1L)
        );

        verify(userMapper, never())
                .toResponse(any(User.class));
    }

    private User createUser(
            Long id,
            String email,
            UserStatus status
    ) {
        User user = new User();

        user.setId(id);
        user.setEmail(email);
        user.setStatus(status);

        return user;
    }
}