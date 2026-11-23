package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.client.UsuarioClient;
import com.ticketya.notificaciones.config.NotificacionesProperties;
import com.ticketya.notificaciones.dto.response.UsuarioResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.Notificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.exception.CircuitoAbiertoException;
import com.ticketya.notificaciones.exception.EnvioEmailException;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.service.impl.NotificacionEnvioServiceImpl;
import com.ticketya.notificaciones.service.impl.PlantillaEmail;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionEnvioServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");

    @Mock
    private NotificacionRepository repository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private EnviadorEmail enviador;

    private NotificacionEnvioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificacionEnvioServiceImpl(repository, usuarioClient, new PlantillaEmail(), enviador,
                new NotificacionesProperties(), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    private static Notificacion notificacion(String destinatario) {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(5L);
        notificacion.setClaveIdempotencia("COMPRA_CONFIRMADA:idEntrada:101");
        notificacion.setIdUsuario(42L);
        notificacion.setTipo(TipoNotificacion.COMPRA_CONFIRMADA);
        notificacion.setDestinatario(destinatario);
        notificacion.setDatos(new HashMap<>(java.util.Map.of(
                "evento", "Ritual en vivo", "fechaHora", "24/10 21:00", "idEntrada", "101")));
        notificacion.setEstado(EstadoNotificacion.ENVIANDO);
        return notificacion;
    }

    /** Simula el reclamo atomico exitoso y la relectura de la fila. */
    private void reclamada(Notificacion notificacion) {
        when(repository.reclamar(eq(5L), eq(EstadoNotificacion.ENVIANDO), anyCollection(), eq(5),
                any(Instant.class), any(Instant.class))).thenReturn(1);
        when(repository.findById(5L)).thenReturn(Optional.of(notificacion));
    }

    @Test
    void envioCorrectoMarcaLaNotificacionEnviada() {
        Notificacion notificacion = notificacion("ana@correo.com");
        reclamada(notificacion);

        service.procesar(5L);

        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(eq("ana@correo.com"), asunto.capture(), cuerpo.capture());
        assertTrue(asunto.getValue().contains("Ritual en vivo"));
        assertTrue(cuerpo.getValue().contains("Entrada Nº: 101"));
        verify(repository).save(notificacion);
        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertEquals(AHORA, notificacion.getFechaEnvio());
        assertNull(notificacion.getProximoIntentoEn());
        assertNull(notificacion.getUltimoError());
        assertEquals(asunto.getValue(), notificacion.getAsunto());
        verifyNoInteractions(usuarioClient);
    }

    @Test
    void siOtroHiloLaReclamoNoHaceNada() {
        when(repository.reclamar(eq(5L), eq(EstadoNotificacion.ENVIANDO), anyCollection(), eq(5),
                any(Instant.class), any(Instant.class))).thenReturn(0);

        service.procesar(5L);

        verify(repository, never()).findById(any());
        verify(repository, never()).save(any(Notificacion.class));
        verifyNoInteractions(enviador, usuarioClient);
    }

    @Test
    void falloDelSmtpProgramaElReintentoConElPrimerBackoff() {
        Notificacion notificacion = notificacion("ana@correo.com");
        reclamada(notificacion);
        doThrow(new EnvioEmailException("conexion rechazada", new RuntimeException()))
                .when(enviador).enviar(anyString(), anyString(), anyString());

        service.procesar(5L);

        verify(repository).save(notificacion);
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertEquals(AHORA.plus(Duration.ofSeconds(10)), notificacion.getProximoIntentoEn());
        assertTrue(notificacion.getUltimoError().contains("conexion rechazada"));
        assertNull(notificacion.getFechaEnvio());
    }

    @Test
    void elBackoffCreceConLosIntentos() {
        Notificacion notificacion = notificacion("ana@correo.com");
        notificacion.setIntentos(2);
        notificacion.setEstado(EstadoNotificacion.ENVIANDO);
        reclamada(notificacion);
        doThrow(new EnvioEmailException("timeout", new RuntimeException()))
                .when(enviador).enviar(anyString(), anyString(), anyString());

        service.procesar(5L);

        assertEquals(3, notificacion.getIntentos());
        assertEquals(AHORA.plus(Duration.ofMinutes(1)), notificacion.getProximoIntentoEn());
    }

    @Test
    void alAgotarLosIntentosQuedaFallidaSinProximoIntento() {
        Notificacion notificacion = notificacion("ana@correo.com");
        notificacion.setIntentos(4);
        reclamada(notificacion);
        doThrow(new EnvioEmailException("timeout", new RuntimeException()))
                .when(enviador).enviar(anyString(), anyString(), anyString());

        service.procesar(5L);

        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertEquals(5, notificacion.getIntentos());
        assertNull(notificacion.getProximoIntentoEn());
    }

    @Test
    void circuitoAbiertoNoConsumeIntentoYReprogramaComoPendiente() {
        Notificacion notificacion = notificacion("ana@correo.com");
        notificacion.setIntentos(1);
        reclamada(notificacion);
        doThrow(new CircuitoAbiertoException("abierto", new RuntimeException()))
                .when(enviador).enviar(anyString(), anyString(), anyString());

        service.procesar(5L);

        verify(repository).save(notificacion);
        assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertEquals(AHORA.plus(Duration.ofSeconds(35)), notificacion.getProximoIntentoEn());
    }

    @Test
    void sinDestinatarioLoConsultaAMsvcUsuarios() {
        Notificacion notificacion = notificacion(null);
        reclamada(notificacion);
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioResponse(42L, "Ana", "ana@correo.com"));

        service.procesar(5L);

        verify(enviador).enviar(eq("ana@correo.com"), anyString(), anyString());
        assertEquals("ana@correo.com", notificacion.getDestinatario());
        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
    }

    @Test
    void usuarioSinEmailDescartaLaNotificacionSinReintentar() {
        Notificacion notificacion = notificacion(null);
        reclamada(notificacion);
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioResponse(42L, "Ana", null));

        service.procesar(5L);

        assertEquals(EstadoNotificacion.DESCARTADA, notificacion.getEstado());
        assertTrue(notificacion.getUltimoError().startsWith("SIN_DESTINATARIO"));
        assertNull(notificacion.getProximoIntentoEn());
        assertEquals(0, notificacion.getIntentos());
        verify(repository).save(notificacion);
        verifyNoInteractions(enviador);
    }

    @Test
    void usuarioInexistenteDescartaLaNotificacion() {
        Notificacion notificacion = notificacion(null);
        reclamada(notificacion);
        when(usuarioClient.obtenerPorId(42L)).thenThrow(Mockito.mock(FeignException.NotFound.class));

        service.procesar(5L);

        assertEquals(EstadoNotificacion.DESCARTADA, notificacion.getEstado());
        assertTrue(notificacion.getUltimoError().startsWith("USUARIO_INEXISTENTE"));
        verifyNoInteractions(enviador);
    }

    @Test
    void siMsvcUsuariosNoRespondeSeCuentaComoIntentoFallido() {
        Notificacion notificacion = notificacion(null);
        reclamada(notificacion);
        when(usuarioClient.obtenerPorId(42L)).thenThrow(new RuntimeException("read timeout"));

        service.procesar(5L);

        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertEquals(1, notificacion.getIntentos());
        assertEquals(AHORA.plus(Duration.ofSeconds(10)), notificacion.getProximoIntentoEn());
        assertTrue(notificacion.getUltimoError().contains("msvc-usuarios"));
        verifyNoInteractions(enviador);
    }

    @Test
    void elCorrelationIdDeLaNotificacionEstaEnElMdcDuranteElEnvio() {
        Notificacion notificacion = notificacion("ana@correo.com");
        notificacion.setCorrelationId("corr-777");
        reclamada(notificacion);
        AtomicReference<String> visto = new AtomicReference<>();
        doAnswer(invocacion -> {
            visto.set(MDC.get("correlationId"));
            return null;
        }).when(enviador).enviar(anyString(), anyString(), anyString());

        service.procesar(5L);

        assertEquals("corr-777", visto.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void buscarElegiblesDelegaEnElRepositorio() {
        when(repository.buscarElegibles(anyCollection(), eq(EstadoNotificacion.ENVIANDO), eq(5),
                any(Instant.class), any(Instant.class), any(Pageable.class))).thenReturn(List.of(1L, 2L));

        assertEquals(List.of(1L, 2L), service.buscarElegibles());
    }
}
