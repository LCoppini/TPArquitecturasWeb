package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.EventoCanceladoEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CancelacionNotificacionListenerTest {

    @Test
    void notificaACadaUsuarioAfectado() {
        NotificacionesClient client = mock(NotificacionesClient.class);

        new CancelacionNotificacionListener(client).publicar(new EventoCanceladoEvent(20L, List.of(10L, 11L), "corr"));

        ArgumentCaptor<NotificacionRequest> captor = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(client, times(2)).enviar(captor.capture());
        assertEquals(List.of(10L, 11L), captor.getAllValues().stream().map(NotificacionRequest::idUsuario).toList());
        assertEquals("EVENTO_CANCELADO", captor.getAllValues().getFirst().tipo());
    }

    @Test
    void siFallaUnEnvioContinuaConLosDemas() {
        NotificacionesClient client = mock(NotificacionesClient.class);
        doThrow(new RuntimeException("caido")).when(client).enviar(any());

        new CancelacionNotificacionListener(client).publicar(new EventoCanceladoEvent(20L, List.of(10L, 11L), "corr"));

        verify(client, times(2)).enviar(any());
    }
}
