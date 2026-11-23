package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.DevolucionAprobadaEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DevolucionNotificacionListenerTest {

    @Test
    void avisaUnaVezConEventoEntradasYMonto() {
        NotificacionesClient client = mock(NotificacionesClient.class);

        new DevolucionNotificacionListener(client).publicar(new DevolucionAprobadaEvent(
                40L, 20L, 10L, List.of(101L, 102L), new BigDecimal("18000.00"), "corr"));

        ArgumentCaptor<NotificacionRequest> captor = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(client).enviar(captor.capture());
        NotificacionRequest pedido = captor.getValue();
        assertEquals("DEVOLUCION_APROBADA", pedido.tipo());
        assertEquals(10L, pedido.idUsuario());
        assertEquals(20L, pedido.datos().get("idEvento"));
        assertEquals(40L, pedido.datos().get("idCompra"));
        assertEquals(List.of(101L, 102L), pedido.datos().get("idsEntradas"));
        assertEquals(new BigDecimal("18000.00"), pedido.datos().get("monto"));
    }

    @Test
    void sinCompraNoIncluyeIdCompra() {
        NotificacionesClient client = mock(NotificacionesClient.class);

        new DevolucionNotificacionListener(client).publicar(new DevolucionAprobadaEvent(
                null, 20L, 10L, List.of(101L), new BigDecimal("500.00"), "corr"));

        ArgumentCaptor<NotificacionRequest> captor = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(client).enviar(captor.capture());
        assertFalse(captor.getValue().datos().containsKey("idCompra"));
    }

    @Test
    void unFalloDeEnvioNoSePropaga() {
        NotificacionesClient client = mock(NotificacionesClient.class);
        doThrow(new RuntimeException("caido")).when(client).enviar(any());

        new DevolucionNotificacionListener(client).publicar(new DevolucionAprobadaEvent(
                40L, 20L, 10L, List.of(101L), new BigDecimal("1.00"), "corr"));

        verify(client).enviar(any());
    }
}
