package ru.bankapi.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserCreationService userCreationService;

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        User user = userCreationService.createUser(request, UserRole.CLIENT);

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = getUserById(userId);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse blockUser(Long userId) {
        User user = getUserById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidOperationException("Администратора нельзя заблокировать");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InvalidOperationException("Пользователь уже заблокирован");
        }

        user.setStatus(UserStatus.BLOCKED);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse unblockUser(Long userId) {
        User user = getUserById(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new InvalidOperationException("Пользователь уже активен");
        }

        user.setStatus(UserStatus.ACTIVE);

        return userMapper.toResponse(user);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}