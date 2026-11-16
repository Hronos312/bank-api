package ru.bankapi.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@ConditionalOnProperty(name = "app.identity.internal-enabled", havingValue = "true")
public class InternalAccessToken {

    public static final String HEADER = "X-Internal-Token";
    private final String value;

    public InternalAccessToken(@Value("${app.identity.internal-token}") String value) {
        if (!value.matches("[A-Za-z0-9_-]{32,128}")) {
            throw new IllegalArgumentException("Требуется отдельный токен внутреннего API длиной от 32 символов");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean matches(String supplied) {
        return supplied != null && MessageDigest.isEqual(
                value.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
