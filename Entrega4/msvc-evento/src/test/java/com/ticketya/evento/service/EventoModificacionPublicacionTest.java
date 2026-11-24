package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.request.EventoRequest;
import com.ticketya.evento.dto.response.LugarResponse;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.event.EventoModificadoEvent;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Al actualizar un evento se avisa del cambio de nombre, fecha o lugar; sin cambios no se avisa. */
@ExtendWith(MockitoExtension.class)
class EventoModificacionPublicacionTest {

    private static final Long ID_EVENTO = 7L;
    private static final Long ORGANIZADOR = 2L;
    private static final Instant FECHA = Instant.parse("2027-01-10T21:00:00Z");

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
        evento.setIdLugar(3L);
        evento.setIdOrganizador(ORGANIZADOR);
        evento.setNombre("Ritual en vivo");
        evento.setDescripcion("Descripcion");
        evento.setTipo("Recital");
        evento.setFechaHora(FECHA);
        evento.setEstado(EstadosEvento.BORRADOR);
        when(eventoRepository.findById(ID_EVENTO)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);
    }

    @Test
    void cambiarLaFechaPublicaElAvisoConLaFechaNuevaYLaAnterior() {
        when(lugarClient.obtenerPorId(3L)).thenReturn(lugar(3L, "Estadio"));

        service.actualizar(ID_EVENTO, ORGANIZADOR, solicitud(3L, "Ritual en vivo", Instant.parse("2027-01-12T21:00:00Z")));

        EventoModificadoEvent publicado = capturar();
        assertEquals(ID_EVENTO, publicado.idEvento());
        assertTrue(publicado.cambios().contains("nueva fecha: 12/01/2027 18:00"), publicado.cambios());
        assertTrue(publicado.cambios().contains("antes 10/01/2027 18:00"), publicado.cambios());
    }

    @Test
    void cambiarNombreYLugarLosDescribeJuntos() {
        when(lugarClient.obtenerPorId(4L)).thenReturn(lugar(4L, "Teatro del Centro"));

        service.actualizar(ID_EVENTO, ORGANIZADOR, solicitud(4L, "Ritual 2", FECHA));

        String cambios = capturar().cambios();
        assertTrue(cambios.contains("nuevo nombre: Ritual 2"), cambios);
        assertTrue(cambios.contains("nuevo lugar: Teatro del Centro"), cambios);
        assertTrue(cambios.contains("; "), cambios);
    }

    @Test
    void sinCambiosDeNombreFechaNiLugarNoAvisa() {
        when(lugarClient.obtenerPorId(3L)).thenReturn(lugar(3L, "Estadio"));

        service.actualizar(ID_EVENTO, ORGANIZADOR, solicitud(3L, "Ritual en vivo", FECHA));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void cambiarSoloLaDescripcionNoAvisa() {
        when(lugarClient.obtenerPorId(3L)).thenReturn(lugar(3L, "Estadio"));
        EventoRequest solicitud = new EventoRequest(3L, "Ritual en vivo", "Otra descripcion", "Recital", FECHA, List.of());

        service.actualizar(ID_EVENTO, ORGANIZADOR, solicitud);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private EventoModificadoEvent capturar() {
        ArgumentCaptor<EventoModificadoEvent> captor = ArgumentCaptor.forClass(EventoModificadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private EventoRequest solicitud(Long idLugar, String nombre, Instant fecha) {
        return new EventoRequest(idLugar, nombre, "Descripcion", "Recital", fecha, List.of());
    }

    private LugarResponse lugar(Long id, String nombre) {
        return new LugarResponse(id, nombre, "Calle 1", null, "Buenos Aires");
    }
}
