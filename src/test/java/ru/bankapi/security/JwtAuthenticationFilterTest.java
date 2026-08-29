package ru.bankapi.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserWhenTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer test-jwt-token"
        );

        UserDetails userDetails = new User(
                "ivan@example.com",
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );

        when(jwtService.extractEmail("test-jwt-token"))
                .thenReturn("ivan@example.com");

        when(userDetailsService.loadUserByUsername("ivan@example.com"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(
                "test-jwt-token",
                "ivan@example.com"
        )).thenReturn(true);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(
                "ivan@example.com",
                authentication.getName()
        );

        assertTrue(
                authentication.getAuthorities()
                        .contains(
                                new SimpleGrantedAuthority(
                                        "ROLE_CLIENT"
                                )
                        )
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsInvalid()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );

        when(jwtService.extractEmail("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(userDetailsService, never())
                .loadUserByUsername(anyString());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenValidationFails()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                "Authorization",
                "Bearer test-jwt-token"
        );

        UserDetails userDetails = new User(
                "ivan@example.com",
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );

        when(jwtService.extractEmail("test-jwt-token"))
                .thenReturn("ivan@example.com");

        when(userDetailsService.loadUserByUsername("ivan@example.com"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(
                "test-jwt-token",
                "ivan@example.com"
        )).thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
    }
}