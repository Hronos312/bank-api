package ru.bankapi.identity;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.bankapi.exception.ServiceUnavailableException;
import ru.bankapi.observability.CorrelationFilter;

import java.time.Duration;
import java.net.http.HttpClient;
import java.util.Optional;

@Slf4j
@Component
class HttpIdentityDirectory implements IdentityDirectory, UserDetailsService {

    private final RestClient client;

    HttpIdentityDirectory(RestClient.Builder builder, InternalAccessToken token,
                          @Value("${app.identity.url}") String identityUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(1));
        this.client = builder.baseUrl(identityUrl).requestFactory(factory)
                .defaultHeader(InternalAccessToken.HEADER, token.value()).build();
    }

    @Override
    public Optional<UserIdentity> findByUsername(String subject) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof BankingPrincipal principal
                && principal.getUsername().equals(subject)) {
            var identity = principal.identity();
            return Optional.of(new UserIdentity(identity.id(), identity.status()));
        }
        return findStatus(subject).map(identity -> new UserIdentity(identity.id(), identity.status()));
    }

    @Override
    public BankingPrincipal loadUserByUsername(String subject) {
        return findStatus(subject).map(BankingPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }

    private Optional<InternalUserStatus> findStatus(String subject) {
        try {
            return Optional.of(request(subject));
        } catch (ResourceAccessException firstFailure) {
            // Повторяем только безопасный запрос чтения после сетевого сбоя.
            try {
                return Optional.of(request(subject));
            } catch (HttpClientErrorException.NotFound exception) {
                return Optional.empty();
            } catch (RestClientException exception) {
                throw unavailable();
            }
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private InternalUserStatus request(String subject) {
        var request = client.get().uri("/internal/users/{id}", subject);
        String correlationId = MDC.get(CorrelationFilter.MDC_KEY);
        if (correlationId != null) {
            request.header(CorrelationFilter.HEADER, correlationId);
        }
        var user = request.retrieve().body(InternalUserStatus.class);
        if (user == null || user.status() == null || user.role() == null
                || !subject.equals(String.valueOf(user.id()))) {
            throw unavailable();
        }
        return user;
    }

    private ServiceUnavailableException unavailable() {
        log.warn("Не удалось проверить статус пользователя в Identity");
        return new ServiceUnavailableException("Сервис проверки пользователей временно недоступен");
    }
}
