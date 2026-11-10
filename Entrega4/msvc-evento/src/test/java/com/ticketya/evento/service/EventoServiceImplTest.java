package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.client.UsuarioClient;
import com.ticketya.evento.dto.request.ConfigurarSectoresRequest;
import com.ticketya.evento.dto.request.EventoRequest;
import com.ticketya.evento.dto.request.EventoSectorRequest;
import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.*;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.AccesoDenegadoException;
import com.ticketya.evento.exception.ReglaNegocioException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {

    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long ADMINISTRADOR_ID = 20L;
    private static final Long LUGAR_ID = 30L;

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private EventoSectorRepository eventoSectorRepository;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private UsuarioClient usuarioClient;
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
                usuarioClient,
                lugarClient,
                sectorClient,
                entradaClient,
                new EventoMapper()
        );
    }

    @Test
    void crearEventoValidoLoGuardaComoBorradorYAsociaOrganizador() {
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(lugarClient.obtenerPorId(LUGAR_ID)).thenReturn(lugar(LUGAR_ID));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacion -> {
            Evento evento = invocacion.getArgument(0);
            evento.setId(1L);
            return evento;
        });

        EventoResponse respuesta = service.crear(ORGANIZADOR_ID, solicitudEvento(List.of()));

        assertEquals(1L, respuesta.id());
        assertEquals(ORGANIZADOR_ID, respuesta.idOrganizador());
        assertEquals(EstadosEvento.BORRADOR, respuesta.estado());
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    void crearEventoReutilizaParticipanteExistentePorNombreYTipo() {
        prepararUsuario(ORGANIZADOR_ID, "ROLE_ORGANIZADOR");
        when(lugarClient.obtenerPorId(LUGAR_ID)).thenReturn(lugar(LUGAR_ID));
        Participante participante = participante(5L, "Banda", "Grupo");
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase("Banda", "Grupo"))
                .thenReturn(Optional.of(participante));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EventoResponse respuesta = service.crear(
                ORGANIZADOR_ID,
                solicitudEvento(List.of(new ParticipanteRequest(null, " Banda ", " Grupo ")))
        );

        assertEquals(1, respuesta.participantes().size());
        assertEquals(5L, respuesta.participantes().getFirst().id());
        verify(participanteRepository, never()).save(any(Participante.class));
    }

    @Test
    void crearEventoRechazaUsuarioSinRolOrganizador() {
        prepararUsuario(ORGANIZADOR_ID, "CLIENTE");

        assertThrows(
                AccesoDenegadoException.class,
                () -> service.crear(ORGANIZADOR_ID, solicitudEvento(List.of()))
        );

        verify(lugarClient, never()).obtenerPorId(any());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoRechazaAOrganizadorQueNoEsPropietario() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, 11L, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");

        assertThrows(
                AccesoDenegadoException.class,
                () -> service.actualizar(1L, ORGANIZADOR_ID, solicitudEvento(List.of()))
        );

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarEventoRechazaCambiosCuandoNoEstaEnBorrador() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");

        assertThrows(
                ReglaNegocioException.class,
                () -> service.actualizar(1L, ORGANIZADOR_ID, solicitudEvento(List.of()))
        );

        verify(lugarClient, never()).obtenerPorId(any());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void actualizarLugarEliminaSectoresConfiguradosParaElLugarAnterior() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        evento.getSectores().add(new EventoSector(evento, 40L, new BigDecimal("100.00")));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(lugarClient.obtenerPorId(31L)).thenReturn(lugar(31L));
        when(eventoRepository.save(evento)).thenReturn(evento);
        EventoRequest solicitud = new EventoRequest(
                31L, "Evento actualizado", "Descripción", "Festival",
                Instant.now().plusSeconds(3600), List.of()
        );

        EventoResponse respuesta = service.actualizar(1L, ORGANIZADOR_ID, solicitud);

        assertEquals(31L, respuesta.idLugar());
        assertEquals(List.of(), respuesta.sectores());
        assertEquals(0, evento.getSectores().size());
        verify(eventoRepository).save(evento);
    }

    @Test
    void configurarSectoresRechazaSectorDuplicado() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(sectorClient.obtenerPorId(40L)).thenReturn(new SectorResponse(40L, LUGAR_ID, "Platea", 100));
        ConfigurarSectoresRequest solicitud = new ConfigurarSectoresRequest(List.of(
                sectorRequest(40L, "100.00"),
                sectorRequest(40L, "120.00")
        ));

        assertThrows(IllegalArgumentException.class, () -> service.configurarSectores(1L, ORGANIZADOR_ID, solicitud));

        verify(sectorClient).obtenerPorId(40L);
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void configurarSectoresRechazaSectorDeOtroLugar() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(sectorClient.obtenerPorId(40L)).thenReturn(new SectorResponse(40L, 999L, "Platea", 100));

        assertThrows(
                ReglaNegocioException.class,
                () -> service.configurarSectores(
                        1L, ORGANIZADOR_ID,
                        new ConfigurarSectoresRequest(List.of(sectorRequest(40L, "100.00")))
                )
        );

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void publicarEventoConSectorValidoCambiaEstadoAPublicado() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        evento.getSectores().add(new EventoSector(evento, 40L, new BigDecimal("100.00")));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(lugarClient.obtenerPorId(LUGAR_ID)).thenReturn(lugar(LUGAR_ID));
        when(sectorClient.obtenerPorId(40L)).thenReturn(new SectorResponse(40L, LUGAR_ID, "Platea", 100));
        when(eventoRepository.save(evento)).thenReturn(evento);

        EventoResponse respuesta = service.publicar(1L, ORGANIZADOR_ID);

        assertEquals(EstadosEvento.PUBLICADO, respuesta.estado());
        verify(eventoRepository).save(evento);
    }

    @Test
    void publicarEventoSinSectoresLoRechaza() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");
        when(lugarClient.obtenerPorId(LUGAR_ID)).thenReturn(lugar(LUGAR_ID));

        assertThrows(ReglaNegocioException.class, () -> service.publicar(1L, ORGANIZADOR_ID));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void suspenderEventoPublicadoRequiereAdministradorYActualizaEstado() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ADMINISTRADOR_ID, "ADMINISTRADOR");
        when(eventoRepository.save(evento)).thenReturn(evento);

        EventoResponse respuesta = service.suspender(1L, ADMINISTRADOR_ID);

        assertEquals(EstadosEvento.SUSPENDIDO, respuesta.estado());
        verify(eventoRepository).save(evento);
    }

    @Test
    void finalizarEventoFuturoLoRechaza() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ADMINISTRADOR_ID, "ADMINISTRADOR");

        assertThrows(
                ReglaNegocioException.class,
                () -> service.cambiarEstadoAdministrativo(1L, ADMINISTRADOR_ID, "FINALIZADO")
        );

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void cancelarEventoGuardaEstadoYCoordinaConEntrada() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ADMINISTRADOR_ID, "ADMINISTRADOR");
        when(eventoRepository.saveAndFlush(evento)).thenReturn(evento);

        EventoResponse respuesta = service.cancelar(1L, ADMINISTRADOR_ID);

        assertEquals(EstadosEvento.CANCELADO, respuesta.estado());
        verify(eventoRepository).saveAndFlush(evento);
        verify(entradaClient).cancelarEntradas(1L, ADMINISTRADOR_ID, "ADMINISTRADOR");
    }

    @Test
    void cancelarConFallaDeEntradaMantieneEventoCanceladoParaPermitirReintento() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ADMINISTRADOR_ID, "ADMINISTRADOR");
        when(eventoRepository.saveAndFlush(evento)).thenReturn(evento);
        doThrow(FeignException.class)
                .when(entradaClient).cancelarEntradas(1L, ADMINISTRADOR_ID, "ADMINISTRADOR");

        assertThrows(ServicioExternoException.class, () -> service.cancelar(1L, ADMINISTRADOR_ID));

        assertEquals(EstadosEvento.CANCELADO, evento.getEstado());
        verify(eventoRepository).saveAndFlush(evento);
    }

    @Test
    void reintentarCoordinacionDeEventoCanceladoVuelveAInvocarEntrada() {
        Evento evento = evento(1L, EstadosEvento.CANCELADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ADMINISTRADOR_ID, "ADMINISTRADOR");

        EventoResponse respuesta = service.reintentarCoordinacionCancelacion(1L, ADMINISTRADOR_ID);

        assertEquals(EstadosEvento.CANCELADO, respuesta.estado());
        verify(entradaClient).cancelarEntradas(1L, ADMINISTRADOR_ID, "ADMINISTRADOR");
    }

    @Test
    void eliminarEventoBorradorPropioEliminaRegistro() {
        Evento evento = evento(1L, EstadosEvento.BORRADOR, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");

        service.eliminarFisicamente(1L, ORGANIZADOR_ID);

        verify(eventoRepository).delete(evento);
    }

    @Test
    void eliminarEventoPublicadoLoRechaza() {
        Evento evento = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        prepararUsuario(ORGANIZADOR_ID, "ORGANIZADOR");

        assertThrows(ReglaNegocioException.class, () -> service.eliminarFisicamente(1L, ORGANIZADOR_ID));

        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    @Test
    void buscarOrdenaPorPrecioMinimoFiltraParticipanteYPaginaResultado() {
        Evento masBarato = evento(1L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(7200));
        masBarato.reemplazarParticipantes(List.of(participante(50L, "Solista", "Música")));
        masBarato.getSectores().add(new EventoSector(masBarato, 41L, new BigDecimal("50.00")));
        masBarato.getSectores().add(new EventoSector(masBarato, 42L, new BigDecimal("80.00")));
        Evento masCaro = evento(2L, EstadosEvento.PUBLICADO, ORGANIZADOR_ID, Instant.now().plusSeconds(3600));
        masCaro.reemplazarParticipantes(List.of(participante(51L, "Solista invitado", "Música")));
        masCaro.getSectores().add(new EventoSector(masCaro, 43L, new BigDecimal("100.00")));
        when(eventoRepository.findAll()).thenReturn(List.of(masCaro, masBarato));

        EventoPaginaResponse respuesta = service.buscar(
                null, null, "solista", null, null, null, "precioAsc", 1, 1
        );

        assertEquals(2, respuesta.totalElementos());
        assertEquals(2, respuesta.totalPaginas());
        assertEquals(1, respuesta.contenido().size());
        assertEquals(2L, respuesta.contenido().getFirst().id());
    }

    @Test
    void buscarRechazaRangoDeFechasInvertido() {
        Instant ahora = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.buscar(null, null, null, null, ahora, ahora.minusSeconds(1), null, 0, 10)
        );

        verify(eventoRepository, never()).findAll();
    }

    private void prepararUsuario(Long id, String rol) {
        when(usuarioClient.obtenerPorId(id)).thenReturn(new UsuarioResponse(id, List.of(rol)));
    }

    private EventoRequest solicitudEvento(List<ParticipanteRequest> participantes) {
        return new EventoRequest(
                LUGAR_ID,
                " Evento ",
                " Descripción ",
                " Festival ",
                Instant.now().plusSeconds(3600),
                participantes
        );
    }

    private EventoSectorRequest sectorRequest(Long idSector, String precio) {
        return new EventoSectorRequest(idSector, new BigDecimal(precio));
    }

    private LugarResponse lugar(Long id) {
        return new LugarResponse(id, "Estadio", "Calle 1", "Centro", "Ciudad");
    }

    private Evento evento(Long id, String estado, Long idOrganizador, Instant fechaHora) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setIdLugar(LUGAR_ID);
        evento.setIdOrganizador(idOrganizador);
        evento.setNombre("Evento " + id);
        evento.setDescripcion("Descripción");
        evento.setTipo("Festival");
        evento.setFechaHora(fechaHora);
        evento.setEstado(estado);
        return evento;
    }

    private Participante participante(Long id, String nombre, String tipo) {
        Participante participante = new Participante(nombre, tipo);
        participante.setId(id);
        return participante;
    }
}
