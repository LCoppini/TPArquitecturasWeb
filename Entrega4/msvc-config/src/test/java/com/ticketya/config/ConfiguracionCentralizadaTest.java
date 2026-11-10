package com.ticketya.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfiguracionCentralizadaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @CsvSource({
            "msvc-usuarios,8081",
            "msvc-ubicacion,8082",
            "msvc-evento,8083",
            "msvc-entrada,8084",
            "msvc-gateway,8080",
            "msvc-eureka,8761"
    })
    void publicaConfiguracionPredeterminadaDeCadaComponente(String application, int port) throws Exception {
        JsonNode source = sourceFor(application);

        assertEquals(application, property(source, "spring.application.name").asText());
        assertEquals(port, property(source, "server.port").asInt());
    }

    @Test
    void publicaRutasDelGateway() throws Exception {
        JsonNode source = sourceFor("msvc-gateway");
        assertEquals("usuarios-route", property(source, "spring.cloud.gateway.server.webmvc.routes[0].id").asText());
        assertEquals("lb://msvc-usuarios", property(source, "spring.cloud.gateway.server.webmvc.routes[0].uri").asText());
        assertEquals("pagos-route", property(source, "spring.cloud.gateway.server.webmvc.routes[7].id").asText());
    }

    @Test
    void publicaConfiguracionDeEurekaServer() throws Exception {
        JsonNode source = sourceFor("msvc-eureka");

        assertFalse(property(source, "eureka.client.register-with-eureka").asBoolean());
        assertFalse(property(source, "eureka.client.fetch-registry").asBoolean());
    }

    @ParameterizedTest
    @CsvSource({
            "msvc-usuarios,ticketya_usuarios",
            "msvc-ubicacion,ticketya_ubicacion",
            "msvc-evento,ticketya_evento",
            "msvc-entrada,ticketya_entrada"
    })
    void publicaLaBaseDeDatosCorrespondiente(String application, String database) throws Exception {
        JsonNode source = sourceFor(application);

        assertEquals(
                "jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/" + database,
                property(source, "spring.datasource.url").asText()
        );
    }

    private JsonNode sourceFor(String application) throws Exception {
        String response = mockMvc.perform(get("/{application}/default", application))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("propertySources").get(0).path("source");
    }

    private JsonNode property(JsonNode source, String propertyName) {
        JsonNode value = source.get(propertyName);
        if (value != null) {
            return value;
        }

        value = source;
        for (String segment : propertyName.split("\\.")) {
            value = value.path(segment);
        }
        return value;
    }
}
