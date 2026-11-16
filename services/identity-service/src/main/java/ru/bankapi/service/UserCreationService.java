package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.model.User;

@Service
@RequiredArgsConstructor
public class UserCreationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateDataException("EMAIL_ALREADY_EXISTS", "Пользователь с таким email уже существует");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateDataException("PHONE_ALREADY_EXISTS", "Пользователь с таким номером телефона уже существует");
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());

        user.setBirthDate(request.getBirthDate());
        user.setPhone(request.getPhone());

        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }
}