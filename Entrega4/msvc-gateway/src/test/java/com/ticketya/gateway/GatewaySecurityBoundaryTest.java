package com.ticketya.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * La cadena de seguridad solo alcanza /api/** y /internal/** (SecurityConfig).
 * /actuator/** y /error deben seguir siendo accesibles sin JWT y sin responder
 * el JSON de 401/403 del gateway.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "management.endpoints.web.exposure.include=health"
        }
)
@Import(GatewaySecurityBoundaryTest.TestJwtConfig.class)
class GatewaySecurityBoundaryTest {

    private static final String TOKEN_VALIDO = "token-boundary";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealthEstaFueraDeLaCadenaDeSeguridad() {
        ResponseEntity<Map> respuesta = restTemplate.getForEntity("/actuator/health", Map.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertNotEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
    }

    @Test
    void errorEstaFueraDeLaCadenaDeSeguridad() {
        ResponseEntity<String> respuesta = restTemplate.getForEntity("/error", String.class);

        assertNotNull(respuesta);
        assertNotEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertNotEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
    }

    @Test
    void unaRutaApiSiSigueExigiendoToken() {
        ResponseEntity<Map> respuesta = restTemplate.getForEntity("/api/no-existe", Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
    }

    /** Decoder de prueba para que el contexto arranque sin crypto real. */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (TOKEN_VALIDO.equals(token)) {
                    return Jwt.withTokenValue(token)
                            .header("alg", "RS256")
                            .claim("idUsuario", "42")
                            .claim("roles", List.of("CLIENTE"))
                            .build();
                }
                throw new BadJwtException("Token de prueba desconocido");
            };
        }
    }
}
