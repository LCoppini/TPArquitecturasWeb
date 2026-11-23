package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.Notificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.exception.RecursoNoEncontradoException;
import com.ticketya.notificaciones.exception.SolicitudInvalidaException;
import com.ticketya.notificaciones.mapper.NotificacionMapper;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.service.impl.NotificacionServiceImpl;
import com.ticketya.notificaciones.service.impl.PlantillaEmail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    private static final String CLAVE = "COMPRA_CONFIRMADA:idEntrada:101";

    @Mock
    private NotificacionRepository repository;

    @Mock
    private NotificacionEnvioService envioService;

    private NotificacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificacionServiceImpl(repository, new NotificacionMapper(), new PlantillaEmail(), envioService);
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    private static SolicitudNotificacionRequest solicitud() {
        return new SolicitudNotificacionRequest(CLAVE, 42L, TipoNotificacion.COMPRA_CONFIRMADA, null,
                Map.of("evento", "Ritual en vivo", "fechaHora", "24/10 21:00", "idEntrada", "101"));
    }

    private static Notificacion existente() {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(3L);
        notificacion.setClaveIdempotencia(CLAVE);
        notificacion.setIdUsuario(42L);
        notificacion.setTipo(TipoNotificacion.COMPRA_CONFIRMADA);
        notificacion.setEstado(EstadoNotificacion.ENVIADA);
        return notificacion;
    }

    private void guardarAsignandoId() {
        when(repository.findByClaveIdempotencia(CLAVE)).thenReturn(Optional.empty());
        when(repository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(7L);
            return notificacion;
        });
    }

    @Test
    void solicitudNuevaQuedaPendienteYDisparaElEnvioAsincrono() {
        guardarAsignandoId();

        NotificacionResponse respuesta = service.solicitar(solicitud());

        assertEquals(7L, respuesta.id());
        assertEquals(EstadoNotificacion.PENDIENTE, respuesta.estado());
        assertEquals(0, respuesta.intentos());
        verify(envioService).enviarAsync(7L);
    }

    @Test
    void guardaLaClaveElUsuarioLosDatosYElCorrelationId() {
        guardarAsignandoId();
        MDC.put("correlationId", "corr-123");

        service.solicitar(solicitud());

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals(CLAVE, guardada.getClaveIdempotencia());
        assertEquals(42L, guardada.getIdUsuario());
        assertEquals(TipoNotificacion.COMPRA_CONFIRMADA, guardada.getTipo());
        assertEquals("Ritual en vivo", guardada.getDatos().get("evento"));
        assertEquals("corr-123", guardada.getCorrelationId());
        assertNull(guardada.getDestinatario());
    }

    @Test
    void guardaElDestinatarioSiViene() {
        guardarAsignandoId();
        SolicitudNotificacionRequest conDestinatario = new SolicitudNotificacionRequest(CLAVE, 42L,
                TipoNotificacion.COMPRA_CONFIRMADA, " ana@correo.com ",
                Map.of("evento", "Ritual en vivo", "fechaHora", "24/10 21:00", "idEntrada", "101"));

        service.solicitar(conDestinatario);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        assertEquals("ana@correo.com", captor.getValue().getDestinatario());
    }

    @Test
    void reintentoConLaMismaClaveDevuelveLaExistenteSinEnviarOtra() {
        when(repository.findByClaveIdempotencia(CLAVE)).thenReturn(Optional.of(existente()));

        NotificacionResponse respuesta = service.solicitar(solicitud());

        assertEquals(3L, respuesta.id());
        assertEquals(EstadoNotificacion.ENVIADA, respuesta.estado());
        verify(repository, never()).save(any(Notificacion.class));
        verify(envioService, never()).enviarAsync(any());
    }

    @Test
    void solicitudesSimultaneasConLaMismaClaveDevuelvenLaGanadora() {
        when(repository.findByClaveIdempotencia(CLAVE)).thenReturn(Optional.empty(), Optional.of(existente()));
        when(repository.save(any(Notificacion.class))).thenThrow(new DataIntegrityViolationException("uk_notificacion_clave"));

        NotificacionResponse respuesta = service.solicitar(solicitud());

        assertEquals(3L, respuesta.id());
        verify(envioService, never()).enviarAsync(any());
    }

    @Test
    void rechazaLaSolicitudSiFaltanDatosObligatoriosDelTipo() {
        SolicitudNotificacionRequest incompleta = new SolicitudNotificacionRequest(CLAVE, 42L,
                TipoNotificacion.COMPRA_CONFIRMADA, null, Map.of("evento", "Ritual en vivo"));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> service.solicitar(incompleta));

        assertTrue(error.getMessage().contains("fechaHora"));
        assertTrue(error.getMessage().contains("idEntrada"));
        verifyNoInteractions(repository, envioService);
    }

    @Test
    void rechazaLaSolicitudSinDatos() {
        SolicitudNotificacionRequest sinDatos = new SolicitudNotificacionRequest(CLAVE, 42L,
                TipoNotificacion.EVENTO_CANCELADO, null, null);

        assertThrows(SolicitudInvalidaException.class, () -> service.solicitar(sinDatos));
        verifyNoInteractions(repository, envioService);
    }

    @Test
    void siElPoolEstaSaturadoLaSolicitudIgualSeAceptaYQuedaPendiente() {
        guardarAsignandoId();
        doThrow(new TaskRejectedException("pool lleno")).when(envioService).enviarAsync(7L);

        NotificacionResponse respuesta = service.solicitar(solicitud());

        assertEquals(EstadoNotificacion.PENDIENTE, respuesta.estado());
    }

    @Test
    void obtenerPorIdInexistenteLanza404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(9L));
    }

    @Test
    void obtenerPorIdDevuelveElDetalle() {
        when(repository.findById(3L)).thenReturn(Optional.of(existente()));

        assertEquals(CLAVE, service.obtenerPorId(3L).claveIdempotencia());
    }

    @Test
    void listarUsaElFiltroQueCorresponde() {
        service.listar(42L, EstadoNotificacion.FALLIDA);
        service.listar(42L, null);
        service.listar(null, EstadoNotificacion.FALLIDA);
        List<NotificacionResponse> sinFiltros = service.listar(null, null);

        verify(repository).findTop100ByIdUsuarioAndEstadoOrderByIdDesc(42L, EstadoNotificacion.FALLIDA);
        verify(repository).findTop100ByIdUsuarioOrderByIdDesc(42L);
        verify(repository).findTop100ByEstadoOrderByIdDesc(EstadoNotificacion.FALLIDA);
        verify(repository).findTop100ByOrderByIdDesc();
        assertTrue(sinFiltros.isEmpty());
    }
}
