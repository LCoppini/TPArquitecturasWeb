package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.dto.request.EventoPublicadoRequest;
import com.ticketya.notificaciones.dto.request.EventoPublicadoRequest.ArtistaEvento;
import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.ClientesDeArtistaResponse;
import com.ticketya.notificaciones.dto.response.EventoPublicadoResponse;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.service.impl.AvisoEventoPublicadoServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvisoEventoPublicadoServiceImplTest {

    private final PreferenciasClient preferencias = mock(PreferenciasClient.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final AvisoEventoPublicadoService service =
            new AvisoEventoPublicadoServiceImpl(preferencias, notificaciones);

    @Test
    void avisaUnaVezPorUsuarioAunqueSigaAVariosArtistas() {
        when(preferencias.clientesDelArtista(1L, "Tandil"))
                .thenReturn(new ClientesDeArtistaResponse(1L, "Tandil", List.of(10L, 11L)));
        when(preferencias.clientesDelArtista(2L, "Tandil"))
                .thenReturn(new ClientesDeArtistaResponse(2L, "Tandil", List.of(11L, 12L)));

        EventoPublicadoResponse respuesta = service.avisar(solicitud());

        assertEquals(3, respuesta.usuariosNotificados());
        ArgumentCaptor<SolicitudNotificacionRequest> captor = ArgumentCaptor.forClass(SolicitudNotificacionRequest.class);
        verify(notificaciones, times(3)).solicitar(captor.capture());
        SolicitudNotificacionRequest usuario11 = captor.getAllValues().stream()
                .filter(s -> s.idUsuario().equals(11L)).findFirst().orElseThrow();
        assertEquals(TipoNotificacion.ARTISTA_FAVORITO_PUBLICA_EVENTO, usuario11.tipo());
        assertEquals("ARTISTA_FAVORITO_PUBLICA_EVENTO:idEvento:7:idUsuario:11", usuario11.claveIdempotencia());
        assertEquals("Banda A", usuario11.datos().get("artista"));
        assertEquals("Copa Ciudad", usuario11.datos().get("evento"));
        assertEquals("Tandil", usuario11.datos().get("ciudad"));
        assertEquals("10/01/2027 18:00", usuario11.datos().get("fechaHora"));
    }

    @Test
    void sinSeguidoresNoSolicitaNada() {
        when(preferencias.clientesDelArtista(any(), any()))
                .thenReturn(new ClientesDeArtistaResponse(1L, "Tandil", List.of()));

        assertEquals(0, service.avisar(solicitud()).usuariosNotificados());
        verify(notificaciones, never()).solicitar(any());
    }

    @Test
    void siPreferenciasFallaElErrorSePropagaParaQueSePuedaReintentar() {
        when(preferencias.clientesDelArtista(any(), any())).thenThrow(new RuntimeException("caido"));

        assertThrows(RuntimeException.class, () -> service.avisar(solicitud()));
        verify(notificaciones, never()).solicitar(any());
    }

    private EventoPublicadoRequest solicitud() {
        return new EventoPublicadoRequest(7L, "Copa Ciudad", "10/01/2027 18:00", "Tandil",
                List.of(new ArtistaEvento(1L, "Banda A"), new ArtistaEvento(2L, "Banda B")));
    }
}
