package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.client.UsuarioClient;
import com.ticketya.notificaciones.config.CorrelationIdFilter;
import com.ticketya.notificaciones.config.NotificacionesConfig;
import com.ticketya.notificaciones.config.NotificacionesProperties;
import com.ticketya.notificaciones.dto.response.UsuarioResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.Notificacion;
import com.ticketya.notificaciones.exception.CircuitoAbiertoException;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.service.EnviadorEmail;
import com.ticketya.notificaciones.service.NotificacionEnvioService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Tres pasos por notificacion: reclamo atomico (UPDATE condicional), envio y registro del resultado.
 * Ninguna llamada externa (usuarios, SMTP) corre dentro de una transaccion de base.
 */
@Service
@Slf4j
public class NotificacionEnvioServiceImpl implements NotificacionEnvioService {

    private static final List<EstadoNotificacion> REINTENTABLES =
            List.of(EstadoNotificacion.PENDIENTE, EstadoNotificacion.FALLIDA);
    private static final int MAX_LARGO_ERROR = 500;

    private final NotificacionRepository repository;
    private final UsuarioClient usuarioClient;
    private final PlantillaEmail plantilla;
    private final EnviadorEmail enviador;
    private final NotificacionesProperties propiedades;
    private final Clock clock;

    public NotificacionEnvioServiceImpl(NotificacionRepository repository,
                                        UsuarioClient usuarioClient,
                                        PlantillaEmail plantilla,
                                        EnviadorEmail enviador,
                                        NotificacionesProperties propiedades,
                                        Clock clock) {
        this.repository = repository;
        this.usuarioClient = usuarioClient;
        this.plantilla = plantilla;
        this.enviador = enviador;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    @Async(NotificacionesConfig.EXECUTOR)
    @Override
    public void enviarAsync(Long id) {
        procesar(id);
    }

    @Override
    public void procesar(Long id) {
        NotificacionesProperties.Envio config = propiedades.getEnvio();
        Instant ahora = clock.instant();

        int reclamadas = repository.reclamar(id, EstadoNotificacion.ENVIANDO, REINTENTABLES,
                config.getIntentosMaximos(), ahora, ahora.minus(config.getStaleTimeout()));
        if (reclamadas == 0) {
            log.debug("Notificacion {} no elegible: ya enviada, en proceso por otro hilo o sin intentos", id);
            return;
        }

        Optional<Notificacion> encontrada = repository.findById(id);
        if (encontrada.isEmpty()) {
            return;
        }
        Notificacion notificacion = encontrada.get();

        String correlationAnterior = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (notificacion.getCorrelationId() != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, notificacion.getCorrelationId());
        }
        try {
            intentar(notificacion);
        } finally {
            if (correlationAnterior != null) {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlationAnterior);
            } else {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        }
    }

    @Override
    public List<Long> buscarElegibles() {
        NotificacionesProperties.Envio config = propiedades.getEnvio();
        Instant ahora = clock.instant();
        return repository.buscarElegibles(REINTENTABLES, EstadoNotificacion.ENVIANDO,
                config.getIntentosMaximos(), ahora, ahora.minus(config.getStaleTimeout()),
                PageRequest.of(0, config.getLote()));
    }

    private void intentar(Notificacion notificacion) {
        String destinatario = notificacion.getDestinatario();
        if (destinatario == null || destinatario.isBlank()) {
            try {
                UsuarioResponse usuario = usuarioClient.obtenerPorId(notificacion.getIdUsuario());
                destinatario = usuario == null ? null : usuario.email();
            } catch (FeignException.NotFound e) {
                descartar(notificacion, "USUARIO_INEXISTENTE: el usuario " + notificacion.getIdUsuario()
                        + " no existe en msvc-usuarios");
                return;
            } catch (RuntimeException e) {
                registrarFallo(notificacion, "No se pudo consultar el destinatario en msvc-usuarios: " + e.getMessage());
                return;
            }
            if (destinatario == null || destinatario.isBlank()) {
                descartar(notificacion, "SIN_DESTINATARIO: el usuario " + notificacion.getIdUsuario()
                        + " no tiene email");
                return;
            }
            notificacion.setDestinatario(destinatario);
        }

        PlantillaEmail.EmailRenderizado email = plantilla.renderizar(notificacion.getTipo(), notificacion.getDatos());
        notificacion.setAsunto(email.asunto());

        try {
            enviador.enviar(destinatario, email.asunto(), email.cuerpo());
        } catch (CircuitoAbiertoException e) {
            reprogramarPorCircuito(notificacion);
            return;
        } catch (RuntimeException e) {
            registrarFallo(notificacion, "Fallo el envio SMTP: " + e.getMessage());
            return;
        }
        registrarEnvio(notificacion);
    }

    private void registrarEnvio(Notificacion notificacion) {
        Instant ahora = clock.instant();
        notificacion.setEstado(EstadoNotificacion.ENVIADA);
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        notificacion.setFechaEnvio(ahora);
        notificacion.setFechaActualizacion(ahora);
        notificacion.setProximoIntentoEn(null);
        notificacion.setUltimoError(null);
        repository.save(notificacion);
        log.info("Notificacion {} ({}) enviada", notificacion.getId(), notificacion.getTipo());
    }

    private void registrarFallo(Notificacion notificacion, String error) {
        Instant ahora = clock.instant();
        int intentos = notificacion.getIntentos() + 1;
        notificacion.setIntentos(intentos);
        notificacion.setEstado(EstadoNotificacion.FALLIDA);
        notificacion.setUltimoError(truncar(error));
        notificacion.setFechaActualizacion(ahora);
        if (intentos >= propiedades.getEnvio().getIntentosMaximos()) {
            notificacion.setProximoIntentoEn(null);
            log.error("Notificacion {} agoto {} intentos: queda FALLIDA ({})", notificacion.getId(), intentos, error);
        } else {
            notificacion.setProximoIntentoEn(ahora.plus(esperaPara(intentos)));
            log.warn("Notificacion {} fallo el intento {} ({}): se reintenta", notificacion.getId(), intentos, error);
        }
        repository.save(notificacion);
    }

    private void descartar(Notificacion notificacion, String motivo) {
        notificacion.setEstado(EstadoNotificacion.DESCARTADA);
        notificacion.setUltimoError(truncar(motivo));
        notificacion.setProximoIntentoEn(null);
        notificacion.setFechaActualizacion(clock.instant());
        repository.save(notificacion);
        log.warn("Notificacion {} descartada: {}", notificacion.getId(), motivo);
    }

    /** Circuito abierto: no se intento enviar, asi que no consume un intento. */
    private void reprogramarPorCircuito(Notificacion notificacion) {
        Instant ahora = clock.instant();
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setUltimoError("Circuito de envio abierto: se reprograma sin consumir intento");
        notificacion.setProximoIntentoEn(ahora.plus(propiedades.getEnvio().getEsperaCircuitoAbierto()));
        notificacion.setFechaActualizacion(ahora);
        repository.save(notificacion);
        log.warn("Notificacion {} reprogramada: circuito de envio abierto", notificacion.getId());
    }

    /** Espera antes del siguiente intento: backoff[intentos - 1]; el ultimo valor se repite. */
    private Duration esperaPara(int intentosRealizados) {
        List<Duration> backoff = propiedades.getEnvio().getBackoff();
        if (backoff == null || backoff.isEmpty()) {
            return Duration.ofSeconds(10);
        }
        int indice = Math.min(intentosRealizados - 1, backoff.size() - 1);
        return backoff.get(Math.max(indice, 0));
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > MAX_LARGO_ERROR ? texto.substring(0, MAX_LARGO_ERROR) : texto;
    }
}
