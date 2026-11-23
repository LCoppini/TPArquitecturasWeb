package com.ticketya.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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

        // ADR-0001: los prefijos por rol se declaran antes que los generales.
        // La asercion no depende del orden, solo de que la ruta exista y apunte
        // al servicio correcto.
        assertEquals("lb://msvc-usuarios", uriDeRutaConId(source, "usuarios-route").asText());
        assertEquals("lb://msvc-usuarios", uriDeRutaConId(source, "admin-usuarios-route").asText());
        assertEquals("lb://msvc-entrada", uriDeRutaConId(source, "pagos-route").asText());
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

    @ParameterizedTest
    @ValueSource(strings = {
            "msvc-usuarios", "msvc-ubicacion", "msvc-evento", "msvc-entrada",
            "msvc-preferencias", "msvc-gateway", "msvc-eureka"
    })
    void publicaSoloHealthSinDetalles(String application) throws Exception {
        JsonNode source = sourceFor(application);

        assertEquals("health", property(source, "management.endpoints.web.exposure.include").asText());
        assertEquals("never", property(source, "management.endpoint.health.show-details").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "msvc-usuarios", "msvc-ubicacion", "msvc-evento", "msvc-entrada",
            "msvc-preferencias", "msvc-gateway"
    })
    void publicaElPerfilDockerConDescubrimientoInterno(String application) throws Exception {
        JsonNode source = sourceFor(application, "docker");

        assertEquals("http://msvc-eureka:8761/eureka/",
                property(source, "eureka.client.service-url.defaultZone").asText());
        assertTrue(property(source, "eureka.instance.prefer-ip-address").asBoolean());
        assertEquals("5s", property(source, "spring.cloud.loadbalancer.cache.ttl").asText());
        assertTrue(property(source, "logging.file.name").asText().startsWith("/app/logs/"));
    }

    @Test
    void publicaElPerfilDockerDelServidorEureka() throws Exception {
        JsonNode source = sourceFor("msvc-eureka", "docker");

        assertFalse(property(source, "eureka.server.enable-self-preservation").asBoolean());
        assertEquals(5000, property(source, "eureka.server.eviction-interval-timer-in-ms").asInt());
        assertFalse(property(source, "eureka.client.register-with-eureka").asBoolean());
        assertFalse(property(source, "eureka.client.fetch-registry").asBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    void elPerfilGlobalNoDesactivaElRegistroEnEureka() throws Exception {
        Map<String, Object> perfil;
        try (InputStream entrada = Files.newInputStream(
                Path.of("src/main/resources/configuration/application-docker.yml"))) {
            perfil = new Yaml().load(entrada);
        }

        Map<String, Object> eureka = (Map<String, Object>) perfil.get("eureka");
        Map<String, Object> client = (Map<String, Object>) eureka.get("client");
        assertFalse(client.containsKey("register-with-eureka"));
        assertFalse(client.containsKey("fetch-registry"));
    }

    private JsonNode sourceFor(String application) throws Exception {
        String response = mockMvc.perform(get("/{application}/default", application))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("propertySources").get(0).path("source");
    }

    /** Merge de todos los propertySources; el primero de la respuesta gana. */
    private JsonNode sourceFor(String application, String profile) throws Exception {
        String response = mockMvc.perform(get("/{application}/{profile}", application, profile))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode propertySources = objectMapper.readTree(response).path("propertySources");
        ObjectNode merged = objectMapper.createObjectNode();
        for (int i = propertySources.size() - 1; i >= 0; i--) {
            JsonNode source = propertySources.get(i).path("source");
            source.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
        }
        return merged;
    }

    private JsonNode uriDeRutaConId(JsonNode source, String id) {
        String prefijo = "spring.cloud.gateway.server.webmvc.routes[";
        for (int i = 0; i < 100; i++) {
            JsonNode idRuta = property(source, prefijo + i + "].id");
            if (idRuta.isMissingNode()) {
                break;
            }
            if (id.equals(idRuta.asText())) {
                return property(source, prefijo + i + "].uri");
            }
        }
        return fail("No existe la ruta del gateway con id " + id);
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
