package ru.bankapi.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.bankapi.model.User;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final PrivateKey signingKey;
    private final long expiration;

    public JwtService(@Value("${jwt.private-key:file:./secrets/jwt-private.pem}") Resource key,
                      @Value("${jwt.expiration:3600000}") long expiration) {
        if (expiration <= 0) {
            throw new IllegalArgumentException("Срок действия JWT должен быть положительным");
        }
        this.signingKey = RsaKeyFiles.readPrivateKey(key);
        this.expiration = expiration;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(JwtVerifier.ISSUER)
                .audience().add(JwtVerifier.AUDIENCE).and()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expiration / 1000;
    }
}
