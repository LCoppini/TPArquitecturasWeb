package com.ticketya.evento.service.impl;

import com.ticketya.evento.client.NotificacionesClient;
import com.ticketya.evento.dto.request.EventoPublicadoAvisoRequest;
import com.ticketya.evento.event.EventoPublicadoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventoPublicadoListenerTest {

    private final EventoPublicadoEvent evento = new EventoPublicadoEvent(7L, "Copa Ciudad", "10/01/2027 18:00",
            "Tandil", List.of(new EventoPublicadoEvent.Artista(5L, "Banda A")));

    @Test
    void enviaElAvisoANotificaciones() {
        NotificacionesClient cliente = mock(NotificacionesClient.class);

        new EventoPublicadoListener(cliente).avisar(evento);

        ArgumentCaptor<EventoPublicadoAvisoRequest> captor = ArgumentCaptor.forClass(EventoPublicadoAvisoRequest.class);
        verify(cliente).eventoPublicado(captor.capture());
        assertEquals(7L, captor.getValue().idEvento());
        assertEquals("Tandil", captor.getValue().ciudad());
        assertEquals(5L, captor.getValue().artistas().getFirst().idArtista());
    }

    @Test
    void unFalloDeNotificacionesNoSePropaga() {
        NotificacionesClient cliente = mock(NotificacionesClient.class);
        doThrow(new RuntimeException("caido")).when(cliente).eventoPublicado(any());

        new EventoPublicadoListener(cliente).avisar(evento);

        verify(cliente).eventoPublicado(any());
    }
}
