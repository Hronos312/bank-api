package ru.bankapi.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.ErrorHandler;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.admin.AdminUserService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorHandler.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getUsersShouldReturnOk()
            throws Exception {

        UserResponse first =
                createResponse(
                        1L,
                        "first@example.com",
                        UserStatus.ACTIVE
                );

        UserResponse second =
                createResponse(
                        2L,
                        "second@example.com",
                        UserStatus.BLOCKED
                );

        when(adminUserService.getUsers())
                .thenReturn(List.of(first, second));

        mockMvc.perform(
                        get("/api/admin/users")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(2)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getUserShouldReturnOk()
            throws Exception {

        UserResponse response =
                createResponse(
                        1L,
                        "ivan@example.com",
                        UserStatus.ACTIVE
                );

        when(adminUserService.getUser(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/admin/users/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("ivan@example.com")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void createUserShouldReturnOk()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setBirthDate(
                LocalDate.of(2000, 1, 1)
        );
        request.setPhone("+79990000009");

        UserResponse response =
                new UserResponse();

        response.setId(10L);
        response.setEmail("new@example.com");
        response.setRole(UserRole.CLIENT);
        response.setStatus(UserStatus.ACTIVE);

        when(
                adminUserService.createUser(
                        any(RegisterRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/admin/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("new@example.com")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("CLIENT")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(adminUserService)
                .createUser(
                        any(RegisterRequest.class)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void createUserShouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setEmail("not-an-email");
        request.setPassword("123");
        request.setFirstName("");
        request.setLastName("");

        mockMvc.perform(
                        post("/api/admin/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verify(
                adminUserService,
                never()
        ).createUser(
                any(RegisterRequest.class)
        );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void blockUserShouldReturnBlockedUser()
            throws Exception {

        UserResponse response =
                createResponse(
                        1L,
                        "ivan@example.com",
                        UserStatus.BLOCKED
                );

        when(adminUserService.blockUser(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/admin/users/1/block"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        verify(adminUserService)
                .blockUser(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void unblockUserShouldReturnActiveUser()
            throws Exception {

        UserResponse response =
                createResponse(
                        1L,
                        "ivan@example.com",
                        UserStatus.ACTIVE
                );

        when(adminUserService.unblockUser(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/admin/users/1/unblock"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(adminUserService)
                .unblockUser(1L);
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void getUserShouldReturnNotFound()
            throws Exception {

        when(adminUserService.getUser(99L))
                .thenThrow(
                        new NotFoundException(
                                "Пользователь не найден"
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/admin/users/99"
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("NOT_FOUND")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void blockUserShouldReturnConflictWhenAlreadyBlocked()
            throws Exception {

        when(adminUserService.blockUser(1L))
                .thenThrow(
                        new InvalidOperationException(
                                "Пользователь уже заблокирован"
                        )
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/users/1/block"
                        )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_OPERATION"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Пользователь уже заблокирован"
                                )
                );
    }

    private UserResponse createResponse(
            Long id,
            String email,
            UserStatus status
    ) {
        UserResponse response =
                new UserResponse();

        response.setId(id);
        response.setEmail(email);
        response.setStatus(status);

        return response;
    }
}