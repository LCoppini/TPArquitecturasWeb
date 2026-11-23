package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.LugarResponse;
import com.ticketya.evento.dto.response.SectorResponse;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.event.EventoPublicadoEvent;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.EventoSectorRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.impl.EventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Al publicar un evento se avisa con sus artistas y la ciudad del lugar. */
@ExtendWith(MockitoExtension.class)
class EventoPublicacionAvisoArtistasTest {

    private static final Long ID_EVENTO = 7L;
    private static final Long ORGANIZADOR = 2L;
    private static final Long ID_LUGAR = 3L;

    @Mock private EventoRepository eventoRepository;
    @Mock private EventoSectorRepository eventoSectorRepository;
    @Mock private ParticipanteRepository participanteRepository;
    @Mock private LugarClient lugarClient;
    @Mock private SectorClient sectorClient;
    @Mock private EntradaClient entradaClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EventoServiceImpl service;
    private Evento evento;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(eventoRepository, eventoSectorRepository, participanteRepository,
                lugarClient, sectorClient, entradaClient, new EventoMapper());
        service.setEventPublisher(eventPublisher);
        evento = new Evento();
        evento.setId(ID_EVENTO);
        evento.setIdLugar(ID_LUGAR);
        evento.setIdOrganizador(ORGANIZADOR);
        evento.setNombre("Copa Ciudad");
        evento.setDescripcion("Descripcion");
        evento.setTipo("Recital");
        evento.setFechaHora(Instant.parse("2027-01-10T21:00:00Z"));
        evento.setEstado(EstadosEvento.BORRADOR);
        evento.getSectores().add(new EventoSector(evento, 10L, new BigDecimal("1000.00")));
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);
        when(lugarClient.obtenerPorId(ID_LUGAR)).thenReturn(new LugarResponse(ID_LUGAR, "Estadio", "Calle 1",
                null, "Tandil"));
        when(sectorClient.obtenerPorId(10L)).thenReturn(new SectorResponse(10L, ID_LUGAR, "Campo", 100, "NO_NUMERADO",
                null, null));
    }

    @Test
    void publicarAvisaConArtistasCiudadYFecha() {
        Participante banda = new Participante("Banda A", "Grupo");
        banda.setId(5L);
        evento.reemplazarParticipantes(List.of(banda));

        service.publicar(ID_EVENTO, ORGANIZADOR);

        ArgumentCaptor<EventoPublicadoEvent> captor = ArgumentCaptor.forClass(EventoPublicadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        EventoPublicadoEvent publicado = captor.getValue();
        assertEquals(ID_EVENTO, publicado.idEvento());
        assertEquals("Copa Ciudad", publicado.evento());
        assertEquals("Tandil", publicado.ciudad());
        assertEquals("10/01/2027 18:00", publicado.fechaHora());
        assertEquals(List.of(new EventoPublicadoEvent.Artista(5L, "Banda A")), publicado.artistas());
    }

    @Test
    void sinParticipantesNoAvisa() {
        service.publicar(ID_EVENTO, ORGANIZADOR);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
