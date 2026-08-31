package ru.bankapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.enums.UserRole;
import ru.bankapi.model.User;
import ru.bankapi.service.UserCreationService;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserCreationService userCreationService;

    @Value("${admin.bootstrap.email:}")
    private String email;

    @Value("${admin.bootstrap.password:}")
    private String password;

    @Value("${admin.bootstrap.first-name:System}")
    private String firstName;

    @Value("${admin.bootstrap.last-name:Administrator}")
    private String lastName;

    @Value("${admin.bootstrap.birth-date:1970-01-01}")
    private String birthDate;

    @Value("${admin.bootstrap.phone:}")
    private String phone;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank() && phone.isBlank()) {
            return;
        }

        validateConfiguration();

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Пользователь с ADMIN_EMAIL уже существует, но не является администратором");
            }

            return;
        }

        RegisterRequest request = new RegisterRequest();

        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setBirthDate(LocalDate.parse(birthDate));
        request.setPhone(phone);

        userCreationService.createUser(request, UserRole.ADMIN);
    }

    private void validateConfiguration() {
        if (email.isBlank()) {
            throw new IllegalStateException("Не задана переменная ADMIN_EMAIL");
        }

        if (password.isBlank()) {
            throw new IllegalStateException("Не задана переменная ADMIN_PASSWORD");
        }

        if (phone.isBlank()) {
            throw new IllegalStateException("Не задана переменная ADMIN_PHONE");
        }
    }
}