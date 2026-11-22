package com.ticketya.gateway;

import com.ticketya.gateway.security.GatewayRoleMatrix;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz completa de autorizacion. Si se agrega una ruta al gateway sin una
 * regla explicita en GatewayRoleMatrix, la prueba de cobertura falla.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@Import(GatewayRoleMatrixTest.TestJwtConfig.class)
class GatewayRoleMatrixTest {

    private static final String CLIENTE = "CLIENTE";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ORGANIZADOR = "ORGANIZADOR";
    private static final String PERSONAL_ACCESO = "PERSONAL_ACCESO";

    @Autowired
    private TestRestTemplate restTemplate;

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosDenegados")
    void rolSinPermisoDevuelve403(String nombre, HttpMethod metodo, String path, String token) {
        ResponseEntity<Map> respuesta = exchange(path, metodo, token);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode(), nombre);
        assertErrorUniforme(respuesta);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosPermitidos")
    void rolConPermisoNoRecibe401Ni403(String nombre, HttpMethod metodo, String path, String token) {
        ResponseEntity<Map> respuesta = exchange(path, metodo, token);

        assertFalse(HttpStatus.UNAUTHORIZED.equals(respuesta.getStatusCode()), nombre);
        assertFalse(HttpStatus.FORBIDDEN.equals(respuesta.getStatusCode()), nombre);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosPublicos")
    void endpointsPublicosNoExigenToken(String nombre, HttpMethod metodo, String path) {
        ResponseEntity<Map> respuesta = exchange(path, metodo, null);

        assertFalse(HttpStatus.UNAUTHORIZED.equals(respuesta.getStatusCode()), nombre);
        assertFalse(HttpStatus.FORBIDDEN.equals(respuesta.getStatusCode()), nombre);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/main/resources/application.yml",
            "../msvc-config/src/main/resources/configuration/msvc-gateway.yml"
    })
    @SuppressWarnings("unchecked")
    void cadaRutaDelGatewayTieneReglaExplicita(String archivo) throws Exception {
        Path ruta = Path.of(archivo);
        Assumptions.assumeTrue(Files.exists(ruta), "No existe el archivo " + archivo);

        Map<String, Object> raiz;
        try (InputStream entrada = Files.newInputStream(ruta)) {
            raiz = new Yaml().load(entrada);
        }
        Map<String, Object> spring = (Map<String, Object>) raiz.get("spring");
        Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
        Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
        Map<String, Object> server = (Map<String, Object>) gateway.get("server");
        Map<String, Object> webmvc = (Map<String, Object>) server.get("webmvc");
        List<Map<String, Object>> rutas = (List<Map<String, Object>>) webmvc.get("routes");
        assertNotNull(rutas, "No hay rutas declaradas en " + archivo);

        List<String> prefijos = GatewayRoleMatrix.prefijosConRegla();
        for (Map<String, Object> definicion : rutas) {
            String id = String.valueOf(definicion.get("id"));
            List<String> predicates = (List<String>) definicion.get("predicates");
            String path = predicates.stream()
                    .filter(predicate -> predicate.startsWith("Path="))
                    .map(predicate -> predicate.substring("Path=".length()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(path, "La ruta " + id + " no declara un predicate Path.");
            assertTrue(prefijos.stream().anyMatch(path::startsWith),
                    "La ruta '" + id + "' (" + path + ") no tiene regla explícita en GatewayRoleMatrix.");
        }
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod metodo, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, metodo, new HttpEntity<>(headers), Map.class);
    }

    private void assertErrorUniforme(ResponseEntity<Map> respuesta) {
        Map<?, ?> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertEquals(4, cuerpo.size());
        assertEquals(respuesta.getStatusCode().value(), cuerpo.get("status"));
        assertNotNull(cuerpo.get("error"));
        assertNotNull(cuerpo.get("message"));
        assertNotNull(cuerpo.get("path"));
    }

    private static Stream<Arguments> casosDenegados() {
        return Stream.of(
                Arguments.of("CLIENTE no puede consultar usuarios", HttpMethod.GET, "/api/usuarios", CLIENTE),
                Arguments.of("ORGANIZADOR no puede administrar usuarios", HttpMethod.GET, "/api/usuarios", ORGANIZADOR),
                Arguments.of("CLIENTE no puede cancelar como administrador", HttpMethod.POST,
                        "/api/admin/entradas/1/cancelacion", CLIENTE),
                Arguments.of("CLIENTE no puede escanear QR", HttpMethod.POST, "/api/accesos/escanear", CLIENTE),
                Arguments.of("ADMINISTRADOR no puede comprar entradas", HttpMethod.POST, "/api/entradas", ADMINISTRADOR),
                Arguments.of("PERSONAL no puede comprar entradas", HttpMethod.POST, "/api/entradas", PERSONAL_ACCESO),
                Arguments.of("CLIENTE no puede publicar eventos", HttpMethod.POST,
                        "/api/organizador/eventos/1/publicar", CLIENTE),
                Arguments.of("ADMINISTRADOR no usa el path de organizador", HttpMethod.POST,
                        "/api/organizador/eventos/1/publicar", ADMINISTRADOR),
                Arguments.of("ORGANIZADOR no puede suspender eventos", HttpMethod.POST,
                        "/api/admin/eventos/1/suspender", ORGANIZADOR),
                Arguments.of("POST a artistas no es publico", HttpMethod.POST, "/api/artistas", ADMINISTRADOR),
                Arguments.of("CLIENTE no puede consultar el reporte", HttpMethod.GET,
                        "/api/organizador/eventos/1/reporte", CLIENTE),
                Arguments.of("ADMINISTRADOR no puede consultar el reporte", HttpMethod.GET,
                        "/api/organizador/eventos/1/reporte", ADMINISTRADOR),
                Arguments.of("PERSONAL no puede consultar el reporte", HttpMethod.GET,
                        "/api/organizador/eventos/1/reporte", PERSONAL_ACCESO),
                Arguments.of("contrato interno siempre denegado", HttpMethod.GET,
                        "/internal/entradas/disponibilidad", CLIENTE)
        );
    }

    private static Stream<Arguments> casosPermitidos() {
        return Stream.of(
                Arguments.of("CLIENTE compra entradas", HttpMethod.POST, "/api/entradas", CLIENTE),
                Arguments.of("ADMINISTRADOR cancela entradas", HttpMethod.POST,
                        "/api/admin/entradas/1/cancelacion", ADMINISTRADOR),
                Arguments.of("PERSONAL escanea QR", HttpMethod.POST, "/api/accesos/escanear", PERSONAL_ACCESO),
                Arguments.of("ADMINISTRADOR consulta usuarios", HttpMethod.GET, "/api/usuarios", ADMINISTRADOR),
                Arguments.of("ORGANIZADOR publica evento", HttpMethod.POST,
                        "/api/organizador/eventos/1/publicar", ORGANIZADOR),
                Arguments.of("ORGANIZADOR consulta el reporte", HttpMethod.GET,
                        "/api/organizador/eventos/1/reporte", ORGANIZADOR),
                Arguments.of("ADMINISTRADOR suspende evento", HttpMethod.POST,
                        "/api/admin/eventos/1/suspender", ADMINISTRADOR),
                Arguments.of("ADMINISTRADOR edita artistas", HttpMethod.POST, "/api/admin/artistas", ADMINISTRADOR),
                Arguments.of("CLIENTE consulta preferencias", HttpMethod.GET, "/api/preferencias", CLIENTE),
                Arguments.of("ADMINISTRADOR consulta eventos", HttpMethod.GET, "/api/eventos/1", ADMINISTRADOR)
        );
    }

    private static Stream<Arguments> casosPublicos() {
        return Stream.of(
                Arguments.of("login", HttpMethod.POST, "/api/usuarios/login"),
                Arguments.of("registro", HttpMethod.POST, "/api/usuarios/registro"),
                Arguments.of("consulta de eventos", HttpMethod.GET, "/api/eventos/1"),
                Arguments.of("disponibilidad", HttpMethod.GET, "/api/eventos/1/disponibilidad"),
                Arguments.of("consulta de artistas", HttpMethod.GET, "/api/artistas")
        );
    }

    /** Decoder de prueba: el token es el propio rol, sin crypto real. */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!List.of(CLIENTE, ADMINISTRADOR, ORGANIZADOR, PERSONAL_ACCESO).contains(token)) {
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
