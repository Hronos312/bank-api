import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

public class GenerateDevSecrets {

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args.length == 0 ? "secrets" : args[0]);
        Files.createDirectories(directory);
        if (Files.exists(directory.resolve("compose.env"))
                || Files.exists(directory.resolve("jwt-private.pem"))
                || Files.exists(directory.resolve("jwt-public.pem"))) {
            throw new IOException("Каталог уже содержит ключи или настройки Bank API");
        }

        var rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(3072);
        var keys = rsa.generateKeyPair();
        write(directory.resolve("jwt-private.pem"), pem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        write(directory.resolve("jwt-public.pem"), pem("PUBLIC KEY", keys.getPublic().getEncoded()));

        var random = new SecureRandom();
        write(directory.resolve("compose.env"), """
                BANKING_DB_PASSWORD=%s
                IDENTITY_DB_PASSWORD=%s
                NOTIFICATION_DB_PASSWORD=%s
                IDENTITY_INTERNAL_TOKEN=%s
                """.formatted(secret(random), secret(random), secret(random), secret(random)));
    }

    private static String secret(SecureRandom random) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String pem(String type, byte[] bytes) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW);
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
    }
}
