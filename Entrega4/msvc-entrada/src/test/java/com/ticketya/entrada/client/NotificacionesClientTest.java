package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.dto.request.SolicitudNotificacionRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Adapta los hechos de entrada al contrato de msvc-notificaciones. */
class NotificacionesClientTest {

    private NotificacionesFeign feign;
    private EventoClient eventoClient;
    private NotificacionesClient client;

    @BeforeEach
    void setUp() {
        feign = mock(NotificacionesFeign.class);
        eventoClient = mock(EventoClient.class);
        client = new NotificacionesClient(feign, eventoClient);
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, 1L, 2L, "Ritual en vivo", null, Instant.parse("2027-01-10T23:00:00Z"), "PUBLICADO",
                null, List.of(), List.of()));
    }

    @Test
    void compraConfirmadaSeTraduceConClaveIdempotenteYDatosDeLaPlantilla() {
        client.enviar(new NotificacionRequest("COMPRA_CONFIRMADA", 10L, "corr",
                Map.of("idCompra", 40L, "idEvento", 20L, "idsEntradas", List.of(101L, 102L))));

        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(feign).solicitar(captor.capture());
        SolicitudNotificacionRequest solicitud = captor.getValue();
        assertEquals("COMPRA_CONFIRMADA:idCompra:40", solicitud.claveIdempotencia());
        assertEquals("COMPRA_CONFIRMADA", solicitud.tipo());
        assertEquals(10L, solicitud.idUsuario());
        assertEquals("Ritual en vivo", solicitud.datos().get("evento"));
        assertEquals("101, 102", solicitud.datos().get("idEntrada"));
        assertEquals("10/01/2027 20:00", solicitud.datos().get("fechaHora"));
    }

    @Test
    void eventoCanceladoUsaUnaClavePorUsuario() {
        client.enviar(new NotificacionRequest("EVENTO_CANCELADO", 10L, "corr", Map.of("idEvento", 20L)));

        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(feign).solicitar(captor.capture());
        assertEquals("EVENTO_CANCELADO:idEvento:20:idUsuario:10", captor.getValue().claveIdempotencia());
    }

    @Test
    void entradaEmitidaNoGeneraCorreo() {
        client.enviar(new NotificacionRequest("ENTRADA_EMITIDA", 10L, "corr",
                Map.of("idCompra", 40L, "idEvento", 20L, "idEntrada", 101L)));

        verify(feign, never()).solicitar(any());
    }
}
