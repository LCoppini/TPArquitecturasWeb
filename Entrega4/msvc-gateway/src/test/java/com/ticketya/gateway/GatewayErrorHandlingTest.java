package com.ticketya.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.gateway.server.webmvc.routes[0].id=backend-unavailable",
                "spring.cloud.gateway.server.webmvc.routes[0].uri=http://localhost:1",
                "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/proxy/**",
                "spring.cloud.gateway.server.webmvc.routes[1].id=service-not-registered",
                "spring.cloud.gateway.server.webmvc.routes[1].uri=lb://msvc-ausente",
                "spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=/sin-instancia/**",
                "spring.cloud.gateway.server.webmvc.routes[2].id=backend-contract",
                "spring.cloud.gateway.server.webmvc.routes[2].uri=http://localhost:${test.backend.port}",
                "spring.cloud.gateway.server.webmvc.routes[2].predicates[0]=Path=/backend/**"
        }
)
class GatewayErrorHandlingTest {

    private static final HttpServer BACKEND = iniciarBackendDePrueba();

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterAll
    static void detenerBackend() {
        BACKEND.stop(0);
        System.clearProperty("test.backend.port");
    }

    @Test
    void responde404UniformeParaPathSinRuta() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/registros-pago", Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertErrorBody(response, "No se encontró una ruta para la solicitud.");
    }

    @Test
    void responde503UniformeCuandoElBackendNoEstaDisponible() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/proxy/recurso", Map.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertErrorBody(response, "El servicio solicitado no está disponible.");
    }

    @Test
    void responde503UniformeCuandoEurekaNoTieneInstancias() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/sin-instancia/recurso", Map.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertErrorBody(response, "El servicio solicitado no está disponible.");
    }

    @Test
    void conservaHeadersYErroresDevueltosPorElServicioDestino() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "42");
        headers.add("X-User-Role", "CLIENTE");

        ResponseEntity<String> response = restTemplate.exchange(
                "/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("X-User-Id=42"));
        assertTrue(response.getBody().contains("X-User-Role=CLIENTE"));
        assertTrue(response.getBody().contains("error-backend"));
    }

    private void assertErrorBody(ResponseEntity<Map> response, String expectedMessage) {
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(4, body.size());
        assertEquals(response.getStatusCode().value(), body.get("status"));
        assertEquals(expectedMessage, body.get("message"));
        assertNotNull(body.get("error"));
        assertNotNull(body.get("path"));
    }

    private static HttpServer iniciarBackendDePrueba() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/backend", exchange -> {
                String userId = exchange.getRequestHeaders().getFirst("X-User-Id");
                String role = exchange.getRequestHeaders().getFirst("X-User-Role");
                String body = "error-backend; X-User-Id=" + userId + "; X-User-Role=" + role;
                byte[] response = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(HttpStatus.CONFLICT.value(), response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            // Inyecta el puerto efímero antes de que Spring enlace las propiedades de rutas.
            System.setProperty("test.backend.port", String.valueOf(server.getAddress().getPort()));
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
