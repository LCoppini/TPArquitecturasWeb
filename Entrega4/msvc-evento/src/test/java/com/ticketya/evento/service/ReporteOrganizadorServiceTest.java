package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.ReporteEventoResponse;
import com.ticketya.evento.dto.response.ReporteSectorResponse;
import com.ticketya.evento.dto.response.ReporteVentasEventoResponse;
import com.ticketya.evento.dto.response.SectorResponse;
import com.ticketya.evento.dto.response.TotalesReporteResponse;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.exception.AccesoDenegadoException;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.exception.ServicioExternoException;
import com.ticketya.evento.repository.EventoRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteOrganizadorServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private SectorClient sectorClient;

    @Mock
    private EntradaClient entradaClient;

    private ReporteOrganizadorService service;

    @BeforeEach
    void setUp() {
        service = new ReporteOrganizadorService(eventoRepository, sectorClient, entradaClient);
    }

    private static Evento evento(long id, long idOrganizador, String estado, Long... idsSectores) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setIdOrganizador(idOrganizador);
        evento.setEstado(estado);
        evento.setSectores(Arrays.stream(idsSectores)
                .map(idSector -> new EventoSector(evento, idSector, BigDecimal.TEN))
                .toList());
        return evento;
    }

    private static ReporteSectorResponse sector(long idSector, long emitidas, long activas,
                                                long devueltas, long enReintegro,
                                                String bruta, String devuelta, String neta) {
        return new ReporteSectorResponse(idSector, emitidas, activas, devueltas, enReintegro,
                new BigDecimal(bruta), new BigDecimal(devuelta), new BigDecimal(neta));
    }

    private static ReporteVentasEventoResponse ventas(Long idEvento, List<ReporteSectorResponse> sectores) {
        long emitidas = sectores.stream().mapToLong(ReporteSectorResponse::entradasEmitidas).sum();
        long activas = sectores.stream().mapToLong(ReporteSectorResponse::entradasActivas).sum();
        long devueltas = sectores.stream().mapToLong(ReporteSectorResponse::entradasDevueltas).sum();
        long enReintegro = sectores.stream().mapToLong(ReporteSectorResponse::enReintegro).sum();
        return new ReporteVentasEventoResponse(idEvento, sectores,
                new TotalesReporteResponse(emitidas, activas, devueltas, enReintegro,
                        new BigDecimal("127000.00"), new BigDecimal("5000.00"), new BigDecimal("122000.00")));
    }

    private void dadoElEvento(Evento evento) {
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
    }

    @Test
    void organizadorDuedoConsultaElReporteConCapacidadYOcupacion() {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L, 2L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of(
                sector(1L, 127, 120, 5, 2, "127000.00", "5000.00", "122000.00")
        )));
        when(sectorClient.obtenerPorId(1L)).thenReturn(new SectorResponse(1L, 3L, "Platea", 200));
        when(sectorClient.obtenerPorId(2L)).thenReturn(new SectorResponse(2L, 3L, "Campo", 100));

        ReporteEventoResponse respuesta = service.reporte(10L, "42");

        assertEquals(10L, respuesta.idEvento());
        assertEquals("PUBLICADO", respuesta.estadoEvento());
        assertEquals(2, respuesta.porSector().size());
        assertEquals("Platea", respuesta.porSector().getFirst().nombreSector());
        assertEquals(200, respuesta.porSector().getFirst().capacidad());
        assertEquals(new BigDecimal("60.00"), respuesta.porSector().getFirst().porcentajeOcupacion());
        assertEquals(127, respuesta.porSector().getFirst().entradasEmitidas());
        assertEquals(120, respuesta.porSector().getFirst().entradasActivas());
        assertEquals(new BigDecimal("122000.00"), respuesta.porSector().getFirst().recaudacionNeta());
        assertEquals(0, respuesta.porSector().get(1).entradasActivas());
        assertEquals(new BigDecimal("0.00"), respuesta.porSector().get(1).porcentajeOcupacion());
        assertEquals(new BigDecimal("40.00"), respuesta.totales().porcentajeOcupacion());
        assertEquals(new BigDecimal("122000.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void otroOrganizadorRecibe403SinConsultarVentas() {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));

        assertThrows(AccesoDenegadoException.class, () -> service.reporte(10L, "99"));
        verify(entradaClient, never()).consultarReporte(any());
    }

    @Test
    void eventoInexistenteRecibe404() {
        when(eventoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.reporte(10L, "42"));
    }

    @Test
    void identidadAusenteVaciaONoNumericaRecibe403SinLeerElEvento() {
        for (String header : new String[]{null, "", "   ", "abc"}) {
            assertThrows(AccesoDenegadoException.class, () -> service.reporte(10L, header));
        }

        verify(eventoRepository, never()).findById(any());
    }

    @Test
    void sectorBorradoSeIncluyeConCeroYNombreNulo() throws Exception {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of(
                sector(1L, 5, 3, 1, 1, "300.00", "100.00", "200.00")
        )));
        when(sectorClient.obtenerPorId(1L)).thenThrow(mock(FeignException.NotFound.class));

        ReporteEventoResponse respuesta = service.reporte(10L, "42");

        assertEquals(0, respuesta.porSector().getFirst().capacidad());
        assertNull(respuesta.porSector().getFirst().nombreSector());
        assertEquals(new BigDecimal("0.00"), respuesta.porSector().getFirst().porcentajeOcupacion());
        assertEquals(3, respuesta.porSector().getFirst().entradasActivas());
        assertEquals(new BigDecimal("200.00"), respuesta.porSector().getFirst().recaudacionNeta());
    }

    @Test
    void error5xxDelSectorDevuelve502SinReporteParcial() throws Exception {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of(
                sector(1L, 5, 3, 0, 0, "300.00", "0.00", "300.00")
        )));
        when(sectorClient.obtenerPorId(1L)).thenThrow(mock(FeignException.class));

        assertThrows(ServicioExternoException.class, () -> service.reporte(10L, "42"));
    }

    @Test
    void capacidadCeroONulaNoPierdeLasCifras() {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of(
                sector(1L, 5, 3, 0, 0, "300.00", "0.00", "300.00")
        )));
        when(sectorClient.obtenerPorId(1L)).thenReturn(new SectorResponse(1L, 3L, "Platea", 0));

        ReporteEventoResponse respuesta = service.reporte(10L, "42");

        assertEquals(new BigDecimal("0.00"), respuesta.porSector().getFirst().porcentajeOcupacion());
        assertEquals(5, respuesta.porSector().getFirst().entradasEmitidas());
        assertEquals(new BigDecimal("300.00"), respuesta.totales().recaudacionBruta());
    }

    @Test
    void falloDeEntradaClientDevuelve502() throws Exception {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenThrow(mock(FeignException.class));

        assertThrows(ServicioExternoException.class, () -> service.reporte(10L, "42"));
    }

    @Test
    void bodyInternoIncompletoDevuelve502() {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(
                new ReporteVentasEventoResponse(99L, List.of(), new TotalesReporteResponse(
                        0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));

        assertThrows(ServicioExternoException.class, () -> service.reporte(10L, "42"));
    }

    @Test
    void sectorConfiguradoSinVentasSiguePresente() {
        dadoElEvento(evento(10L, 42L, "PUBLICADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of()));
        when(sectorClient.obtenerPorId(1L)).thenReturn(new SectorResponse(1L, 3L, "Platea", 100));

        ReporteEventoResponse respuesta = service.reporte(10L, "42");

        assertEquals(1, respuesta.porSector().size());
        assertEquals("Platea", respuesta.porSector().getFirst().nombreSector());
        assertEquals(0, respuesta.porSector().getFirst().entradasActivas());
        assertEquals(new BigDecimal("0.00"), respuesta.porSector().getFirst().porcentajeOcupacion());
    }

    @Test
    void eventoCanceladoDevuelveSnapshotConSoloReembolsosDescontados() {
        dadoElEvento(evento(10L, 42L, "CANCELADO", 1L));
        when(entradaClient.consultarReporte(10L)).thenReturn(ventas(10L, List.of(
                sector(1L, 2, 0, 0, 2, "200.00", "0.00", "200.00")
        )));
        when(sectorClient.obtenerPorId(1L)).thenReturn(new SectorResponse(1L, 3L, "Platea", 200));

        ReporteEventoResponse respuesta = service.reporte(10L, "42");

        assertEquals("CANCELADO", respuesta.estadoEvento());
        assertEquals(2, respuesta.porSector().getFirst().enReintegro());
        assertEquals(new BigDecimal("200.00"), respuesta.totales().recaudacionBruta());
        assertEquals(new BigDecimal("200.00"), respuesta.totales().recaudacionNeta());
    }
}
