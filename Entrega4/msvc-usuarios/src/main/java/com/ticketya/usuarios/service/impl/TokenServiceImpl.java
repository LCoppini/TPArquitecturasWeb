package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Construye el token con los claims exactos que valida el gateway (ADR-0003). */
@Service
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final long expirationSeconds;

    public TokenServiceImpl(
            JwtEncoder jwtEncoder,
            @Value("${jwt.issuer:ticketya}") String issuer,
            @Value("${jwt.audience:ticketya}") String audience,
            @Value("${jwt.expiration-seconds:900}") long expirationSeconds
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_SECONDS debe ser positivo.");
        }
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String emitir(Long idUsuario, Set<String> roles) {
        Instant ahora = Instant.now();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expirationSeconds))
                .claim("idUsuario", idUsuario)
                .claim("roles", new TreeSet<>(roles).stream().toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    @Override
    public long expiracionEnSegundos() {
        return expirationSeconds;
    }
}
