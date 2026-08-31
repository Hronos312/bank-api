package ru.bankapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.config.SecurityConfig;
import ru.bankapi.controller.admin.AdminUserController;
import ru.bankapi.security.RestAccessDeniedHandler;
import ru.bankapi.security.RestAuthenticationEntryPoint;
import ru.bankapi.service.admin.AdminUserService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class AdminApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void adminShouldAccessAdminApi()
            throws Exception {

        when(adminUserService.getUsers())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/users")
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(
            username = "client@example.com",
            roles = "CLIENT"
    )
    void clientShouldReceiveForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/users")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserShouldReceiveUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/users")
                )
                .andExpect(status().isUnauthorized());
    }
}