package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.CompraPagadaEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CompraNotificacionListenerTest {

    @Test
    void publicaUnAgregadoYUnaNotificacionPorEntradaConElMismoCorrelationId() {
        NotificacionesClient client = mock(NotificacionesClient.class);
        CompraNotificacionListener listener = new CompraNotificacionListener(
                client, CircuitBreakerRegistry.ofDefaults()
        );

        listener.publicar(new CompraPagadaEvent(40L, 20L, 10L, "corr-123", List.of(101L, 102L)));

        ArgumentCaptor<NotificacionRequest> captor = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(client, times(3)).enviar(captor.capture());
        List<NotificacionRequest> requests = captor.getAllValues();
        assertEquals("COMPRA_CONFIRMADA", requests.getFirst().tipo());
        assertEquals(List.of(101L, 102L), requests.getFirst().datos().get("idsEntradas"));
        assertEquals(List.of("ENTRADA_EMITIDA", "ENTRADA_EMITIDA"),
                requests.subList(1, 3).stream().map(NotificacionRequest::tipo).toList());
        assertEquals(List.of(101L, 102L), requests.subList(1, 3).stream()
                .map(request -> request.datos().get("idEntrada")).toList());
        assertEquals(List.of("corr-123", "corr-123", "corr-123"),
                requests.stream().map(NotificacionRequest::correlationId).toList());
        assertNull(MDC.get("correlationId"));
    }
}
