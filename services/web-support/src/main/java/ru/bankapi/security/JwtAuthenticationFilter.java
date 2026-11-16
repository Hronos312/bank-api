package ru.bankapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.bankapi.exception.ApiErrorWriter;
import ru.bankapi.exception.ServiceUnavailableException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtVerifier jwtVerifier;
    private final UserDetailsService userDetailsService;
    private final ApiErrorWriter errors;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.isEmpty()) {
            path = request.getRequestURI();
        }
        return path.startsWith("/internal/") || path.startsWith("/actuator/") || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String subject = jwtVerifier.verifySubject(authorization.substring(7));
            var user = userDetailsService.loadUserByUsername(subject);
            if (!user.isEnabled()) {
                errors.write(request, response, 403, "USER_BLOCKED", "Пользователь заблокирован");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        } catch (ServiceUnavailableException exception) {
            errors.write(request, response, 503, "SERVICE_UNAVAILABLE", exception.getMessage());
            return;
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            // Защищённый маршрут вернёт 401 через обработчик Spring Security.
        }
        chain.doFilter(request, response);
    }
}
