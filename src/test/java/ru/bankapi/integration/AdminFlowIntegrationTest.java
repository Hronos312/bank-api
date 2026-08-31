package ru.bankapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz",
        "admin.bootstrap.email=admin@bank.local",
        "admin.bootstrap.password=AdminPassword123",
        "admin.bootstrap.phone=+70000000001",
        "admin.bootstrap.first-name=System",
        "admin.bootstrap.last-name=Administrator",
        "admin.bootstrap.birth-date=2000-01-01"
})
@AutoConfigureMockMvc
class AdminFlowIntegrationTest {

    private static final String ADMIN_EMAIL =
            "admin@bank.local";

    private static final String ADMIN_PASSWORD =
            "AdminPassword123";

    private static final String CLIENT_EMAIL =
            "client@example.com";

    private static final String CLIENT_PASSWORD =
            "password123";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            )
                    .withDatabaseName(
                            "bank_api_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configurePostgres(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completeAdminFlowShouldWork()
            throws Exception {

        String adminToken =
                login(
                        ADMIN_EMAIL,
                        ADMIN_PASSWORD
                );

        Long clientId =
                createClient(adminToken);

        String clientToken =
                login(
                        CLIENT_EMAIL,
                        CLIENT_PASSWORD
                );

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(
                                        "Authorization",
                                        bearer(clientToken)
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        Long accountId =
                createAccount(clientToken);

        Long cardId =
                issueCard(
                        clientToken,
                        accountId
                );

        deposit(
                clientToken,
                accountId,
                "100.00"
        );

        withdraw(
                clientToken,
                accountId,
                "100.00"
        );

        mockMvc.perform(
                        get("/api/admin/transactions")
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[0].type")
                                .value("WITHDRAWAL")
                )
                .andExpect(
                        jsonPath("$.content[1].type")
                                .value("DEPOSIT")
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/cards/{cardId}/block",
                                cardId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/cards/{cardId}/unblock",
                                cardId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/accounts/{accountId}/block",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        mockMvc.perform(
                        post(
                                "/api/accounts/{accountId}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(clientToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "amount": 10.00
                                        }
                                        """)
                )
                .andExpect(
                        status().isConflict()
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/accounts/{accountId}/unblock",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        mockMvc.perform(
                        patch(
                                "/api/admin/users/{userId}/block",
                                clientId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("BLOCKED")
                );

        loginShouldBeForbidden(
                CLIENT_EMAIL,
                CLIENT_PASSWORD
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/users/{userId}/unblock",
                                clientId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        login(
                CLIENT_EMAIL,
                CLIENT_PASSWORD
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/accounts/{accountId}/close",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CLOSED")
                );

        mockMvc.perform(
                        get(
                                "/api/admin/cards/{cardId}",
                                cardId
                        )
                                .header(
                                        "Authorization",
                                        bearer(adminToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CLOSED")
                );
    }

    private Long createClient(
            String adminToken
    ) throws Exception {

        String json = """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Ivan",
                  "lastName": "Client",
                  "birthDate": "2000-01-01",
                  "phone": "+79990000001"
                }
                """.formatted(
                CLIENT_EMAIL,
                CLIENT_PASSWORD
        );

        String response =
                mockMvc.perform(
                                post(
                                        "/api/admin/users"
                                )
                                        .header(
                                                "Authorization",
                                                bearer(adminToken)
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(json)
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.role")
                                        .value("CLIENT")
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("ACTIVE")
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("id")
                .asLong();
    }

    private String login(
            String email,
            String password
    ) throws Exception {

        String json = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                email,
                password
        );

        String response =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(json)
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.tokenType")
                                        .value("Bearer")
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("accessToken")
                .asText();
    }

    private void loginShouldBeForbidden(
            String email,
            String password
    ) throws Exception {

        String json = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                email,
                password
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCOUNT_BLOCKED")
                );
    }

    private Long createAccount(
            String clientToken
    ) throws Exception {

        String response =
                mockMvc.perform(
                                post("/api/accounts")
                                        .header(
                                                "Authorization",
                                                bearer(clientToken)
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("ACTIVE")
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("id")
                .asLong();
    }

    private Long issueCard(
            String clientToken,
            Long accountId
    ) throws Exception {

        String response =
                mockMvc.perform(
                                post(
                                        "/api/accounts/{accountId}/card",
                                        accountId
                                )
                                        .header(
                                                "Authorization",
                                                bearer(clientToken)
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("ACTIVE")
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("id")
                .asLong();
    }

    private void deposit(
            String clientToken,
            Long accountId,
            String amount
    ) throws Exception {

        String json = """
                {
                  "amount": %s,
                  "description": "Admin flow deposit"
                }
                """.formatted(amount);

        mockMvc.perform(
                        post(
                                "/api/accounts/{accountId}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(clientToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("DEPOSIT")
                );
    }

    private void withdraw(
            String clientToken,
            Long accountId,
            String amount
    ) throws Exception {

        String json = """
                {
                  "amount": %s,
                  "description": "Admin flow withdrawal"
                }
                """.formatted(amount);

        mockMvc.perform(
                        post(
                                "/api/accounts/{accountId}/withdraw",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(clientToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("WITHDRAWAL")
                );
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }
}