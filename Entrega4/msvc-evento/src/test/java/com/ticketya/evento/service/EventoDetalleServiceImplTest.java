package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.EventoDetalleResponse;
import com.ticketya.evento.dto.response.LugarResponse;
import com.ticketya.evento.dto.response.SectorResponse;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Detalle de un evento para el cliente: compone el evento con el lugar y los sectores de msvc-ubicacion. */
@ExtendWith(MockitoExtension.class)
class EventoDetalleServiceImplTest {

    private static final Long ID_EVENTO = 7L;
    private static final Long ID_LUGAR = 3L;

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
        service = new EventoServiceImpl(eventoRepository, eventoSectorRepository, participanteRepository,
                lugarClient, sectorClient, entradaClient, new EventoMapper());
    }

    @Test
    void detalleIncluyeLugarYSectoresConPrecioYDimensiones() {
        Evento evento = evento(EstadosEvento.PUBLICADO);
        evento.getSectores().add(new EventoSector(evento, 11L, new BigDecimal("25000.00")));
        evento.getSectores().add(new EventoSector(evento, 10L, new BigDecimal("12000.00")));
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento));
        when(lugarClient.obtenerPorId(ID_LUGAR)).thenReturn(new LugarResponse(ID_LUGAR, "Estadio", "Calle 1",
                "-34.6,-58.4", "Buenos Aires", List.of(
                new SectorResponse(10L, ID_LUGAR, "Campo", 5000, "NO_NUMERADO", null, null),
                new SectorResponse(11L, ID_LUGAR, "Platea", 200, "NUMERADO", 10, 20))));

        EventoDetalleResponse detalle = service.obtenerDetalle(ID_EVENTO);

        assertEquals("Estadio", detalle.lugar().nombre());
        assertEquals("Calle 1", detalle.lugar().direccion());
        assertEquals("Buenos Aires", detalle.lugar().ciudad());
        assertEquals(List.of(10L, 11L), detalle.sectores().stream().map(s -> s.idSector()).toList());
        assertEquals("Campo", detalle.sectores().get(0).nombre());
        assertEquals("NO_NUMERADO", detalle.sectores().get(0).tipo());
        assertNull(detalle.sectores().get(0).filas());
        assertEquals(10, detalle.sectores().get(1).filas());
        assertEquals(20, detalle.sectores().get(1).asientosPorFila());
        assertEquals(new BigDecimal("25000.00"), detalle.sectores().get(1).precio());
    }

    @Test
    void detalleDeEventoNoPublicadoSeDevuelveConSuEstado() {
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento(EstadosEvento.CANCELADO)));
        when(lugarClient.obtenerPorId(ID_LUGAR)).thenReturn(new LugarResponse(ID_LUGAR, "Estadio", "Calle 1",
                null, "Buenos Aires"));

        assertEquals(EstadosEvento.CANCELADO, service.obtenerDetalle(ID_EVENTO).estado());
    }

    @Test
    void sectorQueUbicacionNoDevuelveConservaIdYPrecio() {
        Evento evento = evento(EstadosEvento.PUBLICADO);
        evento.getSectores().add(new EventoSector(evento, 99L, new BigDecimal("100.00")));
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento));
        when(lugarClient.obtenerPorId(ID_LUGAR)).thenReturn(new LugarResponse(ID_LUGAR, "Estadio", "Calle 1",
                null, "Buenos Aires"));

        var sector = service.obtenerDetalle(ID_EVENTO).sectores().getFirst();

        assertEquals(99L, sector.idSector());
        assertNull(sector.nombre());
        assertEquals(new BigDecimal("100.00"), sector.precio());
    }

    @Test
    void detalleDeEventoInexistenteLanzaNoEncontrado() {
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerDetalle(ID_EVENTO));
    }

    @Test
    void siUbicacionFallaElDetalleLanzaErrorDeServicioExterno() {
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento(EstadosEvento.PUBLICADO)));
        when(lugarClient.obtenerPorId(ID_LUGAR)).thenThrow(mock(FeignException.class));

        assertThrows(ServicioExternoException.class, () -> service.obtenerDetalle(ID_EVENTO));
    }

    private Evento evento(String estado) {
        Evento evento = new Evento();
        evento.setId(ID_EVENTO);
        evento.setIdLugar(ID_LUGAR);
        evento.setIdOrganizador(2L);
        evento.setNombre("Ritual en vivo");
        evento.setDescripcion("Descripcion");
        evento.setTipo("Recital");
        evento.setFechaHora(Instant.parse("2027-01-10T21:00:00Z"));
        evento.setEstado(estado);
        evento.reemplazarParticipantes(List.of(participante()));
        return evento;
    }

    private Participante participante() {
        Participante participante = new Participante("Banda", "ARTISTA");
        participante.setId(1L);
        return participante;
    }
}
