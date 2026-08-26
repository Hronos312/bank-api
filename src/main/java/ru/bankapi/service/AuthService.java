package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.mapper.UserMapper;
import ru.bankapi.model.User;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateDataException(
                    "EMAIL_ALREADY_EXISTS",
                    "Пользователь с таким email уже существует"
            );
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateDataException(
                    "PHONE_ALREADY_EXISTS",
                    "Пользователь с таким номером телефона уже существует"
            );
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());

        user.setBirthDate(request.getBirthDate());
        user.setPhone(request.getPhone());

        user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}