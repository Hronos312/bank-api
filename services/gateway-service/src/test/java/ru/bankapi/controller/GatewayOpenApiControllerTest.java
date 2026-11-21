package ru.bankapi.controller;

import org.junit.jupiter.api.Test;
import ru.bankapi.exception.ServiceUnavailableException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayOpenApiControllerTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void preservesIdenticalComponentsFromBothServices() {
        ObjectNode paths = json.createObjectNode();
        ObjectNode components = json.createObjectNode();

        GatewayOpenApiController.merge(paths, components, source("string"));
        GatewayOpenApiController.merge(paths, components, source("string"));

        assertEquals("string", components.at("/schemas/Shared/type").asText());
    }

    @Test
    void rejectsIncompatibleComponentsWithTheSameName() {
        ObjectNode paths = json.createObjectNode();
        ObjectNode components = json.createObjectNode();
        GatewayOpenApiController.merge(paths, components, source("string"));

        assertThrows(ServiceUnavailableException.class,
                () -> GatewayOpenApiController.merge(paths, components, source("integer")));
        assertEquals("string", components.at("/schemas/Shared/type").asText());
    }

    private ObjectNode source(String type) {
        ObjectNode source = json.createObjectNode();
        source.putObject("components").putObject("schemas").putObject("Shared").put("type", type);
        return source;
    }
}
