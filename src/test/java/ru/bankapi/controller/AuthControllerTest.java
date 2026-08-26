package ru.bankapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.service.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@WebMvcTest(AuthController.class)
@Import(ErrorHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

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

        Mockito.when(authService.register(ArgumentMatchers.any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("ivan@example.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Иван"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Иванов"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("CLIENT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"));

        Mockito.verify(authService).register(ArgumentMatchers.any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        RegisterRequest request = createValidRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"));

        Mockito.verify(authService, Mockito.never())
                .register(ArgumentMatchers.any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenPasswordIsTooShort()
            throws Exception {

        RegisterRequest request = createValidRequest();
        request.setPassword("123");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"));

        Mockito.verify(authService, Mockito.never())
                .register(ArgumentMatchers.any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnBadRequestWhenBirthDateIsInFuture()
            throws Exception {

        RegisterRequest request = createValidRequest();
        request.setBirthDate(LocalDate.now().plusDays(1));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"));

        Mockito.verify(authService, Mockito.never())
                .register(ArgumentMatchers.any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        RegisterRequest request = createValidRequest();

        Mockito.when(authService.register(ArgumentMatchers.any(RegisterRequest.class)))
                .thenThrow(
                        new DuplicateDataException(
                                "EMAIL_ALREADY_EXISTS",
                                "Пользователь с таким email уже существует"
                        )
                );

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.code")
                                .value("EMAIL_ALREADY_EXISTS")
                )
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.message")
                                .value(
                                        "Пользователь с таким email уже существует"
                                )
                )
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.path")
                                .value("/api/auth/register")
                );

        Mockito.verify(authService).register(ArgumentMatchers.any(RegisterRequest.class));
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