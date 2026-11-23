package com.ticketya.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidencia criptografica del Resource Server: usa el JwtDecoder real de
 * Spring Boot con un par RS256 generado dentro del test. No hay dobles de
 * JwtDecoder ni claves versionadas. Los 40 tests anteriores siguen intactos.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.gateway.server.webmvc.enabled=false"
        }
)
@AutoConfigureMockMvc
@Import(JwtVerificacionRealTest.IdentidadController.class)
class JwtVerificacionRealTest {

    private static final String ISSUER = "ticketya";
    private static final String AUDIENCE = "ticketya";
    private static final String MENSAJE_401 = "Token ausente, invalido o vencido.";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicBoolean CONTROLADOR_INVOCADO = new AtomicBoolean(false);

    @TempDir
    static Path directorioClaves;

    private static Claves claves;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void configurarDecoderReal(DynamicPropertyRegistry registry)
            throws IOException, GeneralSecurityException {
        claves = Claves.generarEn(directorioClaves);
        registry.add("JWT_PUBLIC_KEY_LOCATION", () -> claves.ubicacionPublica());
        registry.add("JWT_ISSUER", () -> ISSUER);
        registry.add("JWT_AUDIENCE", () -> AUDIENCE);
    }

    @BeforeEach
    void reiniciarControlador() {
        CONTROLADOR_INVOCADO.set(false);
    }

    @Test
    void elDecoderInyectadoEsElRealDeNimbus() {
        assertInstanceOf(NimbusJwtDecoder.class, jwtDecoder,
                "El test no debe reemplazar el JwtDecoder del Resource Server.");
    }

    @Test
    void lasClavesSeGeneranSoloEnElDirectorioTemporal() throws IOException {
        assertTrue(claves.rutaPublica().startsWith(directorioClaves));
        assertTrue(claves.rutaPrivada().startsWith(directorioClaves));
        assertTrue(Files.readString(claves.rutaPrivada()).startsWith("-----BEGIN PRIVATE KEY-----"));
        try (Stream<Path> rutas = Files.walk(Path.of("."))) {
            assertTrue(rutas.noneMatch(ruta -> ruta.toString().endsWith(".pem")),
                    "No se deben versionar archivos .pem.");
        }
    }

    @Test
    void tokenFirmadoConOtraClaveResponde401SinLlamarAlDestino() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair otroPar = generador.generateKeyPair();
        String token = tokenRs256(otroPar, ISSUER, AUDIENCE, Instant.now(), Instant.now().plusSeconds(900));

        assert401(solicitar(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void tokenExpiradoResponde401SinLlamarAlDestino() throws Exception {
        Instant expiracion = Instant.now().minus(1, ChronoUnit.MINUTES);
        String token = tokenRs256(claves.par(), ISSUER, AUDIENCE,
                expiracion.minusSeconds(900), expiracion);

        assert401(solicitar(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void issuerDistintoResponde401SinLlamarAlDestino() throws Exception {
        String token = tokenRs256(claves.par(), "otro-emisor", AUDIENCE,
                Instant.now(), Instant.now().plusSeconds(900));

        assert401(solicitar(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void audienceDistintaResponde401SinLlamarAlDestino() throws Exception {
        String token = tokenRs256(claves.par(), ISSUER, "otra-audiencia",
                Instant.now(), Instant.now().plusSeconds(900));

        assert401(solicitar(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "HS256", "RS384", "RS512"})
    void algoritmoDistintoDeRs256Responde401(String algoritmo) throws Exception {
        assert401(solicitar(tokenConAlgoritmo(algoritmo)));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void tokenVencidoEnGetPublicoResponde401SinLlamarAlDestino() throws Exception {
        Instant expiracion = Instant.now().minus(1, ChronoUnit.MINUTES);
        String token = tokenRs256(claves.par(), ISSUER, AUDIENCE,
                expiracion.minusSeconds(900), expiracion);

        assert401Publico(solicitarPublico(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void tokenDeOtraClaveEnGetPublicoResponde401SinLlamarAlDestino() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair otroPar = generador.generateKeyPair();
        String token = tokenRs256(otroPar, ISSUER, AUDIENCE, Instant.now(), Instant.now().plusSeconds(900));

        assert401Publico(solicitarPublico(token));
        assertFalse(CONTROLADOR_INVOCADO.get());
    }

    @Test
    void idUsuarioNumericoLlegaComoXUserIdYSinXUserRole() throws Exception {
        String token = tokenRs256(claves.par(), ISSUER, AUDIENCE,
                Instant.now(), Instant.now().plusSeconds(900));

        mockMvc.perform(get("/api/identidad")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value("42"))
                .andExpect(jsonPath("$.rol").doesNotExist());

        assertTrue(CONTROLADOR_INVOCADO.get());
    }

    private ResultActions solicitar(String token) throws Exception {
        return mockMvc.perform(get("/api/identidad")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions solicitarPublico(String token) throws Exception {
        return mockMvc.perform(get("/api/eventos/publico-de-test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void assert401Publico(ResultActions resultado) throws Exception {
        resultado
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(MENSAJE_401))
                .andExpect(jsonPath("$.path").value("/api/eventos/publico-de-test"));
    }

    private void assert401(ResultActions resultado) throws Exception {
        resultado
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(MENSAJE_401))
                .andExpect(jsonPath("$.path").value("/api/identidad"));
    }

    /** Firma con Nimbus, igual que msvc-usuarios, usando el par indicado. */
    private String tokenRs256(KeyPair par, String issuer, String audience,
                              Instant emitido, Instant expira) {
        RSAKey clave = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) par.getPublic())
                .privateKey(par.getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(emitido)
                .expiresAt(expira)
                .claim("idUsuario", 42L)
                .claim("roles", List.of("CLIENTE"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    /**
     * Construye a mano los algoritmos prohibidos para que el rechazo se deba al
     * header `alg` y no a una firma RS256 invalida.
     */
    private String tokenConAlgoritmo(String algoritmo) throws Exception {
        long ahora = Instant.now().getEpochSecond();
        Map<String, Object> cabecera = new LinkedHashMap<>();
        cabecera.put("alg", algoritmo);
        cabecera.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("aud", List.of(AUDIENCE));
        claims.put("iat", ahora);
        claims.put("exp", ahora + 900);
        claims.put("idUsuario", 42L);
        claims.put("roles", List.of("CLIENTE"));

        String entrada = base64Url(JSON.writeValueAsBytes(cabecera))
                + "." + base64Url(JSON.writeValueAsBytes(claims));
        String firma = switch (algoritmo) {
            case "none" -> "";
            case "HS256" -> base64Url(hmacSha256(entrada));
            case "RS384" -> base64Url(firmarRsa(entrada, "SHA384withRSA", claves.par().getPrivate()));
            case "RS512" -> base64Url(firmarRsa(entrada, "SHA512withRSA", claves.par().getPrivate()));
            default -> throw new IllegalArgumentException("Algoritmo no previsto: " + algoritmo);
        };
        return entrada + "." + firma;
    }

    private byte[] hmacSha256(String entrada) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("clave-hmac-solo-de-test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(entrada.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] firmarRsa(String entrada, String algoritmo, PrivateKey privada)
            throws GeneralSecurityException {
        Signature firma = Signature.getInstance(algoritmo);
        firma.initSign(privada);
        firma.update(entrada.getBytes(StandardCharsets.UTF_8));
        return firma.sign();
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Endpoint de prueba: expone lo que el filtro de identidad reenvió. */
    @RestController
    static class IdentidadController {

        @GetMapping("/api/eventos/publico-de-test")
        Map<String, String> publico() {
            CONTROLADOR_INVOCADO.set(true);
            return Map.of("ok", "true");
        }

        @GetMapping("/api/identidad")
        Map<String, String> identidad(
                @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                @RequestHeader(value = "X-User-Role", required = false) String rol) {

            CONTROLADOR_INVOCADO.set(true);
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("idUsuario", idUsuario == null ? "" : idUsuario);
            if (rol != null) {
                respuesta.put("rol", rol);
            }
            return respuesta;
        }
    }

    private record Claves(KeyPair par, Path rutaPublica, Path rutaPrivada, String ubicacionPublica) {

        static Claves generarEn(Path directorio) throws GeneralSecurityException, IOException {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();

            Path privada = directorio.resolve("jwt_private.pem");
            Path publica = directorio.resolve("jwt_public.pem");
            Files.writeString(privada, pem(par.getPrivate().getEncoded(), "PRIVATE KEY"));
            Files.writeString(publica, pem(par.getPublic().getEncoded(), "PUBLIC KEY"));

            return new Claves(par, publica, privada, publica.toUri().toString());
        }

        private static String pem(byte[] der, String tipo) {
            String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
            return "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n";
        }
    }
}
