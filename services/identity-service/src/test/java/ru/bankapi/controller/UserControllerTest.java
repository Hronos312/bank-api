package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.enums.UserRole;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.security.JwtAuthenticationFilter;
import ru.bankapi.service.UserService;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(
            username = "ivan@example.com",
            roles = "CLIENT"
    )
    void getProfileShouldReturnCurrentUser() throws Exception {
        UserResponse response = new UserResponse();

        response.setId(1L);
        response.setEmail("ivan@example.com");
        response.setFirstName("Иван");
        response.setLastName("Иванов");
        response.setBirthDate(LocalDate.of(2000, 5, 15));
        response.setPhone("+79991234567");
        response.setRole(UserRole.CLIENT);
        response.setStatus(UserStatus.ACTIVE);

        when(userService.getProfile("ivan@example.com"))
                .thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.email")
                                .value("ivan@example.com")
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Иван")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("CLIENT")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(userService)
                .getProfile("ivan@example.com");
    }
}