package ru.bankapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.auth.AuthResponse;
import ru.bankapi.dto.auth.LoginRequest;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidCredentialsException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(ErrorHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerShouldReturnCreated() throws Exception {
        RegisterRequest request = createValidRequest();

        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setEmail(request.getEmail());
        response.setFirstName(request.getFirstName());
        response.setLastName(request.getLastName());
        response.setMiddleName(request.getMiddleName());
        response.setBirthDate(request.getBirthDate());
        response.setPhone(request.getPhone());
        response.setRole(UserRole.CLIENT);
        response.setStatus(UserStatus.ACTIVE);
        response.setCreatedAt(LocalDateTime.of(2026, 8, 26, 18, 30));

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        RegisterRequest request = createValidRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenPasswordIsTooShort()
            throws Exception {

        RegisterRequest request = createValidRequest();
        request.setPassword("123");

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenBirthDateIsInFuture()
            throws Exception {

        RegisterRequest request = createValidRequest();
        request.setBirthDate(LocalDate.now().plusDays(1));

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        RegisterRequest request = createValidRequest();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new DuplicateDataException(
                                "EMAIL_ALREADY_EXISTS",
                                "Пользователь с таким email уже существует"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("EMAIL_ALREADY_EXISTS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Пользователь с таким email уже существует"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/auth/register")
                );

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void loginShouldReturnOkAndToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse(
                "test-jwt-token",
                "Bearer",
                3600L
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("test-jwt-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(3600)
                );

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid()
            throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(
                        new InvalidCredentialsException(
                                "Неверный email или пароль"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_CREDENTIALS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Неверный email или пароль")
                );
    }

    @Test
    void loginShouldReturnBadRequestWhenEmailIsInvalid()
            throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("not-email");
        request.setPassword("password123");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR")
                );

        verify(authService, never())
                .login(any(LoginRequest.class));
    }

    private RegisterRequest createValidRequest() {
        RegisterRequest request = new RegisterRequest();

        request.setEmail("ivan@example.com");
        request.setPassword("password123");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setMiddleName("Иванович");
        request.setBirthDate(LocalDate.of(2000, 5, 15));
        request.setPhone("+79991234567");

        return request;
    }
}