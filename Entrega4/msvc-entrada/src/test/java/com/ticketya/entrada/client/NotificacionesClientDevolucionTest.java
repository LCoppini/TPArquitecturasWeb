package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.dto.request.SolicitudNotificacionRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Traduccion de DEVOLUCION_APROBADA y EVENTO_MODIFICADO al contrato de msvc-notificaciones. */
class NotificacionesClientDevolucionTest {

    private NotificacionesFeign feign;
    private NotificacionesClient client;

    @BeforeEach
    void setUp() {
        feign = mock(NotificacionesFeign.class);
        EventoClient eventoClient = mock(EventoClient.class);
        client = new NotificacionesClient(feign, eventoClient);
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, 1L, 2L, "Ritual en vivo", null, Instant.parse("2027-01-10T23:00:00Z"), "PUBLICADO",
                null, List.of(), List.of()));
    }

    @Test
    void devolucionDeCompraUsaClavePorCompraYDatosDeLaPlantilla() {
        client.enviar(new NotificacionRequest("DEVOLUCION_APROBADA", 10L, "corr",
                Map.of("idCompra", 40L, "idEvento", 20L, "idsEntradas", List.of(101L, 102L),
                        "monto", new BigDecimal("18000.00"))));

        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(feign).solicitar(captor.capture());
        SolicitudNotificacionRequest solicitud = captor.getValue();
        assertEquals("DEVOLUCION_APROBADA:idCompra:40", solicitud.claveIdempotencia());
        assertEquals("DEVOLUCION_APROBADA", solicitud.tipo());
        assertEquals("Ritual en vivo", solicitud.datos().get("evento"));
        assertEquals("101, 102", solicitud.datos().get("idEntrada"));
        assertTrue(solicitud.datos().get("monto").contains("18.000,00"), solicitud.datos().get("monto"));
    }

    @Test
    void eventoModificadoUsaClavePorUsuarioYCambios() {
        client.enviar(new NotificacionRequest("EVENTO_MODIFICADO", 10L, "corr",
                Map.of("idEvento", 20L, "cambios", "fecha: nueva")));

        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(feign).solicitar(captor.capture());
        SolicitudNotificacionRequest solicitud = captor.getValue();
        assertEquals("EVENTO_MODIFICADO", solicitud.tipo());
        assertEquals("Ritual en vivo", solicitud.datos().get("evento"));
        assertEquals("fecha: nueva", solicitud.datos().get("cambios"));
        assertTrue(solicitud.claveIdempotencia().startsWith("EVENTO_MODIFICADO:idEvento:20:idUsuario:10:"));
    }

    @Test
    void devolucionIndividualUsaClavePorEntrada() {
        client.enviar(new NotificacionRequest("DEVOLUCION_APROBADA", 10L, "corr",
                Map.of("idEvento", 20L, "idsEntradas", List.of(101L), "monto", new BigDecimal("500.00"))));

        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(feign).solicitar(captor.capture());
        assertEquals("DEVOLUCION_APROBADA:idEntrada:101", captor.getValue().claveIdempotencia());
    }
}
