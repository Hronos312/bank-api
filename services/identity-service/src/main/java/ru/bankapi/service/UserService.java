package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return userMapper.toResponse(user);
    }
}