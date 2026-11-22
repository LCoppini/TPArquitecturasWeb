package com.ticketya.entrada.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraDatosContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializeEventoConTipoYParticipantes() throws Exception {
        EventoResponse evento = objectMapper.readValue("""
                {"id_evento":20,"nombre":"Recital","tipo":"SHOW",
                 "fecha_hora":"2026-12-01T20:00:00Z","estado":"PUBLICADO",
                 "participantes":[{"idParticipante":7,"nombre":"Banda","tipo":"Banda"}]}
                """, EventoResponse.class);

        assertEquals(20L, evento.id());
        assertEquals("SHOW", evento.tipo());
        assertEquals(Instant.parse("2026-12-01T20:00:00Z"), evento.fechaHora());
        assertEquals(1, evento.participantes().size());
        assertEquals(7L, evento.participantes().get(0).id());
        assertEquals("Banda", evento.participantes().get(0).nombre());
    }

    @Test
    void deserializeUsuarioConCiudad() throws Exception {
        UsuarioResponse usuario = objectMapper.readValue("""
                {"id_usuario":42,"dni":123,"nombre":"Ana","ciudad":"Tandil","activo":true}
                """, UsuarioResponse.class);

        assertEquals(42L, usuario.idUsuario());
        assertEquals("Tandil", usuario.ciudad());
    }

    @Test
    void losConstructoresSobrecargadosSiguenDisponibles() {
        EventoResponse eventoPrevio = new EventoResponse(20L, Instant.now(), "PUBLICADO");
        UsuarioResponse usuarioPrevio = new UsuarioResponse(42L);

        assertEquals(20L, eventoPrevio.id());
        assertEquals(null, eventoPrevio.tipo());
        assertTrue(eventoPrevio.participantes().isEmpty());
        assertEquals(42L, usuarioPrevio.idUsuario());
        assertEquals(null, usuarioPrevio.ciudad());
    }
}
