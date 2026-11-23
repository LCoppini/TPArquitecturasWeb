package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.service.impl.PlantillaEmail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantillaArtistaFavoritoTest {

    private final PlantillaEmail plantilla = new PlantillaEmail();

    @Test
    void exigeArtistaEventoFechaYCiudad() {
        assertEquals(List.of("artista", "evento", "fechaHora", "ciudad"),
                plantilla.faltantes(TipoNotificacion.ARTISTA_FAVORITO_PUBLICA_EVENTO, null));
    }

    @Test
    void renderizaAsuntoYCuerpo() {
        var email = plantilla.renderizar(TipoNotificacion.ARTISTA_FAVORITO_PUBLICA_EVENTO, Map.of(
                "artista", "Banda A", "evento", "Copa Ciudad", "fechaHora", "10/01/2027 18:00", "ciudad", "Tandil"));

        assertEquals("Banda A publicó un evento en Tandil", email.asunto());
        assertTrue(email.cuerpo().contains("Evento: Copa Ciudad"));
        assertTrue(email.cuerpo().contains("Fecha y hora: 10/01/2027 18:00"));
    }
}
