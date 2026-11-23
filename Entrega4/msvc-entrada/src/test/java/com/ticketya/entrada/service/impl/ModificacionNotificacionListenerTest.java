package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.EventoModificadoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ModificacionNotificacionListenerTest {

    @Test
    void notificaACadaUsuarioConLosCambios() {
        NotificacionesClient client = mock(NotificacionesClient.class);

        new ModificacionNotificacionListener(client).publicar(
                new EventoModificadoEvent(20L, List.of(10L, 11L), "fecha: nueva", "corr"));

        ArgumentCaptor<NotificacionRequest> captor = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(client, times(2)).enviar(captor.capture());
        assertEquals(List.of(10L, 11L), captor.getAllValues().stream().map(NotificacionRequest::idUsuario).toList());
        assertEquals("EVENTO_MODIFICADO", captor.getAllValues().getFirst().tipo());
        assertEquals("fecha: nueva", captor.getAllValues().getFirst().datos().get("cambios"));
    }

    @Test
    void siFallaUnEnvioContinuaConLosDemas() {
        NotificacionesClient client = mock(NotificacionesClient.class);
        doThrow(new RuntimeException("caido")).when(client).enviar(any());

        new ModificacionNotificacionListener(client).publicar(
                new EventoModificadoEvent(20L, List.of(10L, 11L), "fecha: nueva", "corr"));

        verify(client, times(2)).enviar(any());
    }
}
