package com.ticketya.gateway;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** GET /api/usuarios/me lo puede consultar cualquier usuario autenticado; el resto de /api/usuarios sigue siendo de ADMIN. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@Import(GatewayPerfilPropioTest.TestJwtConfig.class)
class GatewayPerfilPropioTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "ORGANIZADOR", "ADMINISTRADOR", "PERSONAL_ACCESO"})
    void cualquierRolAutenticadoPuedeConsultarSuPerfil(String rol) {
        HttpStatus estado = estado("/api/usuarios/me", rol);

        assertNotEquals(HttpStatus.UNAUTHORIZED, estado);
        assertNotEquals(HttpStatus.FORBIDDEN, estado);
    }

    @Test
    void sinTokenElPerfilPropioDevuelve401() {
        assertEquals(HttpStatus.UNAUTHORIZED, estado("/api/usuarios/me", null));
    }

    @Test
    void unClienteNoPuedeConsultarOtroUsuarioPorId() {
        assertEquals(HttpStatus.FORBIDDEN, estado("/api/usuarios/5", "CLIENTE"));
    }

    private HttpStatus estado(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<Map> respuesta = restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return HttpStatus.valueOf(respuesta.getStatusCode().value());
    }

    /** Decoder de prueba: el token es el propio rol, sin crypto real. */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!List.of("CLIENTE", "ORGANIZADOR", "ADMINISTRADOR", "PERSONAL_ACCESO").contains(token)) {
                    throw new BadJwtException("Token de prueba desconocido");
                }
                return Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .claim("idUsuario", "42")
                        .claim("roles", List.of(token))
                        .build();
            };
        }
    }
}
