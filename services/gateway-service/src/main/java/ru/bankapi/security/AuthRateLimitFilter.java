package ru.bankapi.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.bankapi.exception.ApiErrorWriter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(-110)
class AuthRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(1)).build();
    private final ApiErrorWriter errors;
    private final int perMinute;

    AuthRateLimitFilter(ApiErrorWriter errors, @Value("${app.auth.requests-per-minute:30}") int perMinute) {
        if (perMinute < 1) {
            throw new IllegalArgumentException("Лимит запросов авторизации должен быть положительным");
        }
        this.errors = errors;
        this.perMinute = perMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equals(request.getMethod())
                || (!"/api/auth/login".equals(path) && !"/api/auth/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int count = attempts.get(request.getRemoteAddr(), address -> new AtomicInteger()).incrementAndGet();
        if (count > perMinute) {
            response.setHeader("Retry-After", "60");
            errors.write(request, response, 429, "RATE_LIMIT", "Слишком много запросов. Повторите позже");
            return;
        }
        chain.doFilter(request, response);
    }
}
