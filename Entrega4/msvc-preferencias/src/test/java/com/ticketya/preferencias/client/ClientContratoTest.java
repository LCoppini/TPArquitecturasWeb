package com.ticketya.preferencias.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketya.preferencias.client.dto.EventoBusquedaResponse;
import com.ticketya.preferencias.client.dto.UsuarioItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientContratoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializaTipoYParticipantesDeLaBusquedaDeEventosConAliases() throws Exception {
        EventoBusquedaResponse respuesta = objectMapper.readValue("""
                {"contenido":[{"id_evento":20,"tipo":"SHOW","fecha_hora":"2026-12-01T20:00:00Z",
                  "estado":"PUBLICADO","participantes":[{"idParticipante":7,"nombre":"Banda","tipo":"Banda"}]}],
                 "pagina":0,"tamano":10,"totalElementos":1,"totalPaginas":1}
                """, EventoBusquedaResponse.class);

        assertEquals(20L, respuesta.contenido().get(0).id());
        assertEquals("SHOW", respuesta.contenido().get(0).tipo());
        assertEquals(7L, respuesta.contenido().get(0).participantes().get(0).id());
        assertEquals("Banda", respuesta.contenido().get(0).participantes().get(0).nombre());
        assertEquals("Banda", respuesta.contenido().get(0).participantes().get(0).tipo());
    }

    @Test
    void deserializaLaCiudadDelUsuarioConAliases() throws Exception {
        UsuarioItem usuario = objectMapper.readValue("""
                {"id_usuario":42,"dni":123,"nombre":"Ana","ciudad":"Tandil","activo":true}
                """, UsuarioItem.class);

        assertEquals(42L, usuario.idUsuario());
        assertEquals("Tandil", usuario.ciudad());
    }
}
