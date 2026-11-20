package com.ticketya.usuarios.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/** Configura la firma RS256 con la clave privada entregada solo por entorno (ADR-0003). */
@Configuration
public class JwtEncoderConfig {

    private static final String CABECERA_PKCS8 = "-----BEGIN PRIVATE KEY-----";

    @Bean
    JwtEncoder jwtEncoder(@Value("${jwt.private-key}") String privateKeyPem) {
        try {
            PrivateKey privateKey = privateKeyFromPem(privateKeyPem);
            if (!(privateKey instanceof RSAPrivateCrtKey privada)) {
                throw new IllegalStateException("JWT_PRIVATE_KEY debe ser una clave RSA PKCS#8.");
            }
            RSAPublicKey publica = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            RSAKey rsaKey = new RSAKey.Builder(publica).privateKey(privada).build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo construir el firmante JWT RS256.", exception);
        }
    }

    /** Visible para tests: decodifica PKCS#8 sin leer archivos ni claves del repositorio. */
    static PrivateKey privateKeyFromPem(String privateKeyPem) {
        String pem = privateKeyPem.replace("\\n", "\n");
        if (!pem.contains(CABECERA_PKCS8)) {
            throw new IllegalArgumentException("JWT_PRIVATE_KEY debe ser PKCS#8 (BEGIN PRIVATE KEY).");
        }
        String base64 = pem.lines()
                .map(String::trim)
                .filter(linea -> !linea.startsWith("-----"))
                .reduce("", String::concat);
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT_PRIVATE_KEY no es un PEM PKCS#8 válido.", exception);
        }
    }
}
