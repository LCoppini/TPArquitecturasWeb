package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.EventoPaginaResponse;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.ReporteVentasEventoResponse;
import com.ticketya.evento.dto.response.TotalesReporteResponse;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.exception.ServicioExternoException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.EventoSectorRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.impl.EventoServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Evidencia del contrato interno nuevo en cantidadVendida(), usado por la búsqueda por relevancia. */
@ExtendWith(MockitoExtension.class)
class EventoServiceImplRelevanciaTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private EventoSectorRepository eventoSectorRepository;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private LugarClient lugarClient;
    @Mock
    private SectorClient sectorClient;
    @Mock
    private EntradaClient entradaClient;

    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(
                eventoRepository,
                eventoSectorRepository,
                participanteRepository,
                lugarClient,
                sectorClient,
                entradaClient,
                new EventoMapper()
        );
    }

    @Test
    void laRelevanciaOrdenaPorEntradasActivasDelReporteInterno() {
        Evento conMasEmitidas = evento(1L, Instant.now().plusSeconds(7200));
        Evento conMasActivas = evento(2L, Instant.now().plusSeconds(3600));
        when(eventoRepository.findAll()).thenReturn(List.of(conMasEmitidas, conMasActivas));
        when(entradaClient.consultarReporte(1L)).thenReturn(reporte(1L, 100, 2));
        when(entradaClient.consultarReporte(2L)).thenReturn(reporte(2L, 1, 50));

        EventoPaginaResponse respuesta = service.buscar(
                null, null, null, null, null, null, "relevancia", 0, 10
        );

        assertEquals(List.of(2L, 1L), respuesta.contenido().stream().map(EventoResponse::id).toList());
        verify(entradaClient).consultarReporte(1L);
        verify(entradaClient).consultarReporte(2L);
    }

    @Test
    void falloDelReporteInternoEnLaRelevanciaSeMapeaA502() throws Exception {
        when(eventoRepository.findAll()).thenReturn(List.of(evento(1L, Instant.now().plusSeconds(3600))));
        when(entradaClient.consultarReporte(1L)).thenThrow(mock(FeignException.class));

        assertThrows(ServicioExternoException.class, () -> service.buscar(
                null, null, null, null, null, null, "relevancia", 0, 10
        ));
    }

    private static Evento evento(Long id, Instant fechaHora) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setIdLugar(30L);
        evento.setIdOrganizador(10L);
        evento.setNombre("Evento " + id);
        evento.setDescripcion("Descripción");
        evento.setTipo("Festival");
        evento.setFechaHora(fechaHora);
        evento.setEstado(EstadosEvento.PUBLICADO);
        return evento;
    }

    private static ReporteVentasEventoResponse reporte(long idEvento, long emitidas, long activas) {
        return new ReporteVentasEventoResponse(idEvento, List.of(),
                new TotalesReporteResponse(emitidas, activas, 0, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
