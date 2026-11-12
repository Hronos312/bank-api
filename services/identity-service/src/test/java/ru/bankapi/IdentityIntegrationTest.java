package ru.bankapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.identity.InternalAccessToken;
import ru.bankapi.security.JwtVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.identity.internal-token=identity-test-token-12345678901234567890")
@AutoConfigureMockMvc
@Testcontainers
class IdentityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) throws Exception {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        Path publicKey = writeKey("PUBLIC KEY", pair.getPublic().getEncoded());
        Path privateKey = writeKey("PRIVATE KEY", pair.getPrivate().getEncoded());
        registry.add("jwt.public-key", () -> publicKey.toUri().toString());
        registry.add("jwt.private-key", () -> privateKey.toUri().toString());
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtVerifier jwt;
    @Autowired
    private InternalAccessToken token;
    @Autowired
    private tools.jackson.databind.ObjectMapper json;

    @Test
    void loginIssuesRsaTokenAndBlockingRevokesExistingAccess() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"identity@example.test","password":"synthetic-password-123",
                "firstName":"Имя","lastName":"Фамилия","birthDate":"1990-01-01","phone":"+79990000001"}
                """)).andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"identity@example.test","password":"synthetic-password-123"}
                """)).andExpect(status().isOk()).andReturn();
        String accessToken = json.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();
        var user = users.findByEmail("identity@example.test").orElseThrow();
        assertEquals(user.getId().toString(), jwt.verifySubject(accessToken));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String internalPath = "/internal/users/" + user.getId();
        mvc.perform(get(internalPath)).andExpect(status().isUnauthorized());
        mvc.perform(get(internalPath).header(InternalAccessToken.HEADER, token.value()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        user.setStatus(UserStatus.BLOCKED);
        users.save(user);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_BLOCKED"));
    }

    private static Path writeKey(String name, byte[] bytes) throws Exception {
        Path path = Files.createTempFile("bank-api-test-", ".pem");
        path.toFile().deleteOnExit();
        Files.writeString(path, "-----BEGIN " + name + "-----\n"
                + Base64.getEncoder().encodeToString(bytes) + "\n-----END " + name + "-----\n");
        return path;
    }
}
