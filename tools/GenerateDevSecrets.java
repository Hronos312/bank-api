import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
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
        var random = new SecureRandom();
        List<Path> created = new ArrayList<>();
        try {
            write(directory.resolve("jwt-private.pem"), pem("PRIVATE KEY", keys.getPrivate().getEncoded()), created);
            write(directory.resolve("jwt-public.pem"), pem("PUBLIC KEY", keys.getPublic().getEncoded()), created);
            write(directory.resolve("compose.env"), """
                    BANKING_DB_PASSWORD=%s
                    IDENTITY_DB_PASSWORD=%s
                    NOTIFICATION_DB_PASSWORD=%s
                    IDENTITY_INTERNAL_TOKEN=%s
                    """.formatted(secret(random), secret(random), secret(random), secret(random)), created);
        } catch (IOException | RuntimeException failure) {
            for (Path file : created.reversed()) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException | RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
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

    private static void write(Path file, String content, List<Path> created) throws IOException {
        if (Files.getFileStore(file.getParent()).supportsFileAttributeView("posix")) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
        } else {
            Files.createFile(file);
        }
        created.add(file);
        Files.writeString(file, content, StandardCharsets.US_ASCII, StandardOpenOption.WRITE);
    }
}
