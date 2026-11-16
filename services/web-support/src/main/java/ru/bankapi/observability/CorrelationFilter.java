package ru.bankapi.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        String id = received != null && received.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
                ? received : UUID.randomUUID().toString();
        response.setHeader(HEADER, id);
        try (var ignored = MDC.putCloseable(MDC_KEY, id)) {
            filterChain.doFilter(new CorrelatedRequest(request, id), response);
        }
    }

    private static final class CorrelatedRequest extends HttpServletRequestWrapper {

        private final String correlationId;

        private CorrelatedRequest(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            return HEADER.equalsIgnoreCase(name) ? correlationId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HEADER.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(correlationId)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            var names = new ArrayList<>(Collections.list(super.getHeaderNames()));
            names.removeIf(HEADER::equalsIgnoreCase);
            names.add(HEADER);
            return Collections.enumeration(names);
        }
    }
}
