package com.ticketya.usuarios.service;

import com.ticketya.usuarios.service.impl.TokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    @Test
    void emiteTokenConLosClaimsDelContrato() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(Jwt.withTokenValue("firmado")
                        .header("alg", "RS256")
                        .claim("idUsuario", 42)
                        .build());
        TokenServiceImpl service = new TokenServiceImpl(encoder, "ticketya", "ticketya", 900);

        String token = service.emitir(42L, Set.of("ORGANIZADOR", "CLIENTE"));

        assertEquals("firmado", token);
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());
        JwtEncoderParameters parametros = captor.getValue();

        assertEquals("RS256", parametros.getJwsHeader().getAlgorithm().getName());
        assertEquals("JWT", parametros.getJwsHeader().getType());
        assertEquals("ticketya", parametros.getClaims().getClaimAsString("iss"));
        assertEquals(List.of("ticketya"), parametros.getClaims().getAudience());
        assertEquals(Duration.ofSeconds(900),
                Duration.between(parametros.getClaims().getIssuedAt(), parametros.getClaims().getExpiresAt()));
        assertEquals(42L, ((Number) parametros.getClaims().getClaim("idUsuario")).longValue());
        assertEquals(List.of("CLIENTE", "ORGANIZADOR"), parametros.getClaims().getClaim("roles"));
    }

    @Test
    void rechazaExpiracionNoPositiva() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        assertThrows(IllegalArgumentException.class, () -> new TokenServiceImpl(encoder, "ticketya", "ticketya", 0));
    }
}
