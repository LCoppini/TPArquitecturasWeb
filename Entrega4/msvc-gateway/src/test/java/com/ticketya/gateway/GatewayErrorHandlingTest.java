package com.ticketya.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Errores uniformes del gateway mas la identidad derivada del JWT (ADR-0001).
 * La asercion anterior de X-User-Role se reemplazo porque el header se elimino
 * por decision de diseno; no se cambio un test para ocultar un fallo.
 */
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
                "spring.cloud.gateway.server.webmvc.routes[2].predicates[0]=Path=/api/backend/**"
        }
)
@Import(GatewayErrorHandlingTest.TestJwtConfig.class)
class GatewayErrorHandlingTest {

    private static final String TOKEN_VALIDO = "token-valido";
    private static final String TOKEN_SIN_ID_USUARIO = "token-sin-id-usuario";

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
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/registros-pago",
                HttpMethod.GET,
                new HttpEntity<>(autorizado(TOKEN_VALIDO)),
                Map.class
        );

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
    void conservaErroresDevueltosPorElServicioDestino() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(autorizado(TOKEN_VALIDO)),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("error-backend"));
        assertTrue(response.getBody().contains("X-User-Id=42"));
    }

    @Test
    void elBackendRecibeElIdUsuarioDelClaimDelJwt() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(autorizado(TOKEN_VALIDO)),
                String.class
        );

        assertTrue(response.getBody().contains("X-User-Id=42"));
    }

    @Test
    void elBackendNoRecibeXUserRoleAunqueElClienteLoEnvie() {
        HttpHeaders headers = autorizado(TOKEN_VALIDO);
        headers.add("X-User-Role", "ADMINISTRADOR");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertFalse(response.getBody().contains("X-User-Role"));
    }

    @Test
    void elXUserIdDelClienteEsSobrescritoPorElDelToken() {
        HttpHeaders headers = autorizado(TOKEN_VALIDO);
        headers.add("X-User-Id", "99");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertTrue(response.getBody().contains("X-User-Id=42"));
        assertFalse(response.getBody().contains("X-User-Id=99"));
    }

    @Test
    void tokenSinClaimIdUsuarioResponde401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/backend/recurso",
                HttpMethod.GET,
                new HttpEntity<>(autorizado(TOKEN_SIN_ID_USUARIO)),
                Map.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertErrorBody(response, "El token no contiene el claim idUsuario.");
    }

    @Test
    void sinTokenEnRutaProtegidaResponde401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/backend/recurso", Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertErrorBody(response, "Token ausente, invalido o vencido.");
    }

    private HttpHeaders autorizado(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
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
            server.createContext("/api/backend", exchange -> {
                String userId = exchange.getRequestHeaders().getFirst("X-User-Id");
                String role = exchange.getRequestHeaders().getFirst("X-User-Role");

                StringBuilder cuerpo = new StringBuilder("error-backend; X-User-Id=" + userId);
                if (role != null) {
                    cuerpo.append("; X-User-Role=").append(role);
                }

                byte[] response = cuerpo.toString().getBytes(StandardCharsets.UTF_8);
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

    /** Decoder de prueba: tokens concretos con los claims del contrato, sin crypto real. */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (GatewayErrorHandlingTest.TOKEN_VALIDO.equals(token)) {
                    return Jwt.withTokenValue(token)
                            .header("alg", "RS256")
                            .claim("idUsuario", "42")
                            .claim("roles", List.of("CLIENTE"))
                            .build();
                }
                if (GatewayErrorHandlingTest.TOKEN_SIN_ID_USUARIO.equals(token)) {
                    return Jwt.withTokenValue(token)
                            .header("alg", "RS256")
                            .claim("roles", List.of("CLIENTE"))
                            .build();
                }
                throw new BadJwtException("Token de prueba desconocido");
            };
        }
    }
}
