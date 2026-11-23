package com.ticketya.evento.service.impl;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.dto.request.ModificacionEventoRequest;
import com.ticketya.evento.event.EventoModificadoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventoModificadoListenerTest {

    @Test
    void avisaAEntradaConLosCambios() {
        EntradaClient entradaClient = mock(EntradaClient.class);

        new EventoModificadoListener(entradaClient).publicar(new EventoModificadoEvent(7L, "nueva fecha: x"));

        ArgumentCaptor<ModificacionEventoRequest> captor = ArgumentCaptor.forClass(ModificacionEventoRequest.class);
        verify(entradaClient).notificarModificacion(org.mockito.ArgumentMatchers.eq(7L), captor.capture());
        assertEquals("nueva fecha: x", captor.getValue().cambios());
    }

    @Test
    void unFalloDeEntradaNoSePropaga() {
        EntradaClient entradaClient = mock(EntradaClient.class);
        doThrow(new RuntimeException("caido")).when(entradaClient).notificarModificacion(any(), any());

        new EventoModificadoListener(entradaClient).publicar(new EventoModificadoEvent(7L, "nueva fecha: x"));

        verify(entradaClient).notificarModificacion(any(), any());
    }
}
