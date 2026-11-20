package com.ticketya.usuarios.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtEncoderConfigTest {

    @Test
    void firmaConPkcs8YLaClavePublicaPuedeValidarElToken() throws Exception {
        KeyPair par = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pem = pemPkcs8(par);

        JwtEncoder encoder = new JwtEncoderConfig().jwtEncoder(pem);
        Instant ahora = Instant.now();
        String token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("ticketya")
                .audience(List.of("ticketya"))
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(900))
                .claim("idUsuario", 7L)
                .claim("roles", List.of("CLIENTE"))
                .build())).getTokenValue();

        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) par.getPublic()).build();
        Jwt decodificado = decoder.decode(token);

        assertEquals("RS256", decodificado.getHeaders().get("alg"));
        assertEquals(7L, ((Number) decodificado.getClaim("idUsuario")).longValue());
        assertEquals(List.of("CLIENTE"), decodificado.getClaimAsStringList("roles"));
    }

    @Test
    void rechazaPemQueNoEsPkcs8() {
        assertThrows(IllegalArgumentException.class,
                () -> JwtEncoderConfig.privateKeyFromPem("-----BEGIN RSA PRIVATE KEY-----\nABC\n-----END RSA PRIVATE KEY-----"));
    }

    private String pemPkcs8(KeyPair par) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(par.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }
}
