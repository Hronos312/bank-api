package ru.bankapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.bankapi.config.SecurityConfig;
import ru.bankapi.exception.ApiErrorWriter;
import ru.bankapi.controller.admin.AdminUserController;
import ru.bankapi.security.RestAccessDeniedHandler;
import ru.bankapi.security.RestAuthenticationEntryPoint;
import ru.bankapi.service.admin.AdminUserService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        ApiErrorWriter.class,
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
    private JwtVerifier jwtVerifier;

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
