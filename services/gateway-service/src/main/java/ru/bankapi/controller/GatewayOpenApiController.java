package ru.bankapi.controller;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.bankapi.exception.ServiceUnavailableException;

import java.time.Duration;

@Hidden
@RestController
class GatewayOpenApiController {

    private final RestClient client;
    private final ObjectMapper json;
    private final String identity;
    private final String banking;

    GatewayOpenApiController(RestClient.Builder builder, ObjectMapper json,
            @Value("${IDENTITY_URL:http://localhost:8081}") String identity,
            @Value("${BANKING_URL:http://localhost:8082}") String banking) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).build();
        this.json = json;
        this.identity = identity;
        this.banking = banking;
    }

    @GetMapping("/v3/api-docs")
    ObjectNode apiDocs() {
        ObjectNode document = json.createObjectNode();
        document.put("openapi", "3.1.0");
        document.putObject("info").put("title", "Bank API").put("version", "1.0");
        document.putArray("servers").addObject().put("url", "/");
        var paths = document.putObject("paths");
        var components = document.putObject("components");
        merge(paths, components, load(identity + "/openapi/identity"));
        merge(paths, components, load(banking + "/openapi/banking"));
        return document;
    }

    private ObjectNode load(String url) {
        try {
            ObjectNode body = client.get().uri(url).retrieve().body(ObjectNode.class);
            if (body != null) {
                return body;
            }
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Описание API временно недоступно");
        }
        throw new ServiceUnavailableException("Сервис вернул пустое описание API");
    }

    private void merge(ObjectNode paths, ObjectNode components, ObjectNode source) {
        if (source.get("paths") instanceof ObjectNode sourcePaths) {
            paths.setAll(sourcePaths);
        }
        source.path("components").properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode values) {
                components.withObjectProperty(entry.getKey()).setAll(values);
            }
        });
    }
}
