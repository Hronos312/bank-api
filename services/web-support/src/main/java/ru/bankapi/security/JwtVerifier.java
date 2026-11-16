package ru.bankapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class JwtVerifier {

    public static final String ISSUER = "bank-api-identity";
    public static final String AUDIENCE = "bank-api";

    private final JwtParser currentKey;
    private final JwtParser previousKey;

    public JwtVerifier(@Value("${jwt.public-key:file:./secrets/jwt-public.pem}") Resource publicKey,
                       @Value("${jwt.previous-public-key:}") String previousKeyPath,
                       ResourceLoader resourceLoader) {
        this.currentKey = parser(publicKey);
        this.previousKey = previousKeyPath.isBlank() ? null : parser(resourceLoader.getResource(previousKeyPath));
    }

    public String verifySubject(String token) {
        Claims claims;
        try {
            claims = currentKey.parseSignedClaims(token).getPayload();
        } catch (io.jsonwebtoken.security.SignatureException exception) {
            if (previousKey == null) {
                throw exception;
            }
            claims = previousKey.parseSignedClaims(token).getPayload();
        }

        String subject = claims.getSubject();
        if (claims.getExpiration() == null || subject == null || !subject.matches("[1-9][0-9]{0,18}")) {
            throw new JwtException("JWT не содержит действительный срок или идентификатор пользователя");
        }
        try {
            Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new JwtException("Идентификатор пользователя в JWT вне допустимого диапазона");
        }
        return subject;
    }

    private JwtParser parser(Resource key) {
        return Jwts.parser().verifyWith(RsaKeyFiles.readPublicKey(key))
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .sig().clear().add(Jwts.SIG.RS256).and()
                .build();
    }
}
