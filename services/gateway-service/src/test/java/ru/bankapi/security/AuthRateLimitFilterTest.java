package ru.bankapi.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.bankapi.exception.ApiErrorWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {

    @Test
    void limitsOnlyAuthenticationRequestsFromTheSameAddress() throws Exception {
        var errors = mock(ApiErrorWriter.class);
        var chain = mock(FilterChain.class);
        var filter = new AuthRateLimitFilter(errors, 1);
        var first = request("/api/auth/login", "192.0.2.1");
        var firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, chain);
        verify(chain).doFilter(first, firstResponse);

        var repeated = request("/api/auth/login", "192.0.2.1");
        var limited = new MockHttpServletResponse();
        filter.doFilter(repeated, limited, chain);
        verify(errors).write(repeated, limited, 429, "RATE_LIMIT", "Слишком много запросов. Повторите позже");
        assertEquals("60", limited.getHeader("Retry-After"));

        var other = request("/api/auth/login", "192.0.2.2");
        var otherResponse = new MockHttpServletResponse();
        filter.doFilter(other, otherResponse, chain);
        verify(chain).doFilter(other, otherResponse);

        var money = request("/api/accounts/1/deposit", "192.0.2.1");
        var moneyResponse = new MockHttpServletResponse();
        filter.doFilter(money, moneyResponse, chain);
        verify(chain).doFilter(money, moneyResponse);
    }

    private MockHttpServletRequest request(String path, String address) {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(address);
        return request;
    }
}
