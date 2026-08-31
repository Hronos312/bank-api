package ru.bankapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dal.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=c29tZS12ZXJ5LWxvbmctdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtb25seS11c2VkLWluLXRlc3Rz"
})
@AutoConfigureMockMvc
class UserFlowIntegrationTest {

    private static final String PASSWORD =
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

    @Autowired
    private BankTransactionRepository
            transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private BankAccountRepository
            bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completeClientFlowShouldWork()
            throws Exception {

        registerUser(
                "ivan@example.com",
                "+79990000001",
                "Ivan"
        );

        registerUser(
                "petr@example.com",
                "+79990000002",
                "Petr"
        );

        String ivanToken =
                login(
                        "ivan@example.com"
                );

        String petrToken =
                login(
                        "petr@example.com"
                );

        AccountData ivanAccount =
                createAccount(ivanToken);

        AccountData petrAccount =
                createAccount(petrToken);

        deposit(
                ivanToken,
                ivanAccount.id(),
                "1000.00"
        );

        transfer(
                ivanToken,
                ivanAccount.id(),
                petrAccount.accountNumber(),
                "250.00"
        );

        BigDecimal ivanBalance =
                getAccountBalance(
                        ivanToken,
                        ivanAccount.id()
                );

        BigDecimal petrBalance =
                getAccountBalance(
                        petrToken,
                        petrAccount.id()
                );

        assertEquals(
                0,
                new BigDecimal("750.00")
                        .compareTo(ivanBalance)
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(petrBalance)
        );

        mockMvc.perform(
                        get(
                                "/api/accounts/{accountId}/transactions",
                                ivanAccount.id()
                        )
                                .header(
                                        "Authorization",
                                        bearer(ivanToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].type")
                                .value("TRANSFER")
                )
                .andExpect(
                        jsonPath("$[1].type")
                                .value("DEPOSIT")
                );

        String reportJson =
                mockMvc.perform(
                                get(
                                        "/api/reports/spending"
                                )
                                        .header(
                                                "Authorization",
                                                bearer(ivanToken)
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode report =
                objectMapper.readTree(
                        reportJson
                );

        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(
                                report.get(
                                        "withdrawals"
                                ).decimalValue()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(
                                report.get(
                                        "outgoingTransfers"
                                ).decimalValue()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(
                                report.get(
                                        "totalSpent"
                                ).decimalValue()
                        )
        );
    }

    private void registerUser(
            String email,
            String phone,
            String firstName
    ) throws Exception {

        String json = """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "%s",
                  "lastName": "Test",
                  "birthDate": "2000-01-01",
                  "phone": "%s"
                }
                """.formatted(
                email,
                PASSWORD,
                firstName,
                phone
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(email)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("CLIENT")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    private String login(
            String email
    ) throws Exception {

        String json = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                email,
                PASSWORD
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

    private AccountData createAccount(
            String token
    ) throws Exception {

        String response =
                mockMvc.perform(
                                post("/api/accounts")
                                        .header(
                                                "Authorization",
                                                bearer(token)
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.currency")
                                        .value("RUB")
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("ACTIVE")
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode account =
                objectMapper.readTree(
                        response
                );

        return new AccountData(
                account.get("id").asLong(),
                account.get(
                        "accountNumber"
                ).asText()
        );
    }

    private void deposit(
            String token,
            Long accountId,
            String amount
    ) throws Exception {

        String json = """
                {
                  "amount": %s,
                  "description": "Integration test deposit"
                }
                """.formatted(amount);

        mockMvc.perform(
                        post(
                                "/api/accounts/{accountId}/deposit",
                                accountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(token)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.type")
                                .value("DEPOSIT")
                );
    }

    private void transfer(
            String token,
            Long sourceAccountId,
            String destinationAccountNumber,
            String amount
    ) throws Exception {

        String json = """
                {
                  "destinationAccountNumber": "%s",
                  "amount": %s,
                  "description": "Integration test transfer"
                }
                """.formatted(
                destinationAccountNumber,
                amount
        );

        mockMvc.perform(
                        post(
                                "/api/accounts/{sourceAccountId}/transfer",
                                sourceAccountId
                        )
                                .header(
                                        "Authorization",
                                        bearer(token)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.type")
                                .value("TRANSFER")
                );
    }

    private BigDecimal getAccountBalance(
            String token,
            Long accountId
    ) throws Exception {

        String response =
                mockMvc.perform(
                                get(
                                        "/api/accounts/{accountId}",
                                        accountId
                                )
                                        .header(
                                                "Authorization",
                                                bearer(token)
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("balance")
                .decimalValue();
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }

    private record AccountData(
            Long id,
            String accountNumber
    ) {
    }
}