package ru.bankapi.security;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyFiles {

    private RsaKeyFiles() {
    }

    public static PublicKey readPublicKey(Resource resource) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(readPem(resource, "PUBLIC KEY")));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Не удалось загрузить открытый ключ JWT", exception);
        }
    }

    public static PrivateKey readPrivateKey(Resource resource) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(resource, "PRIVATE KEY")));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Не удалось загрузить закрытый ключ JWT", exception);
        }
    }

    private static byte[] readPem(Resource resource, String type) throws IOException {
        String pem = resource.getContentAsString(StandardCharsets.US_ASCII).strip();
        String start = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        if (!pem.startsWith(start) || !pem.endsWith(end)) {
            throw new IllegalArgumentException("Неверный формат ключа RSA");
        }
        return Base64.getDecoder().decode(pem.substring(start.length(), pem.length() - end.length())
                .replaceAll("\\s+", ""));
    }
}
