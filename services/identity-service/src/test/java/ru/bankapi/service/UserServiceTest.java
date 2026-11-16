package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getProfileShouldReturnUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result =
                userService.getProfile(email);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(email, result.getEmail());

        verify(userRepository).findByEmail(email);
        verify(userMapper).toResponse(user);
    }

    @Test
    void getProfileShouldThrowWhenUserDoesNotExist() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.getProfile(email)
        );

        verify(userRepository).findByEmail(email);

        verify(userMapper, never())
                .toResponse(any(User.class));
    }
}