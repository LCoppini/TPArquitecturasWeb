package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.config.OutboxProperties;
import com.ticketya.entrada.dto.request.ArtistaCompraRequest;
import com.ticketya.entrada.dto.request.CompraRegistradaRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.service.PreferenciasGateway;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Fase 2 y 3 del outbox: enriquece, llama a preferencias y guarda el resultado. */
@Component
public class PreferenciasOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(PreferenciasOutboxWorker.class);

    private final PendientePreferenciaRepository repository;
    private final EventoClient eventoClient;
    private final UsuarioClient usuarioClient;
    private final PreferenciasGateway gateway;
    private final OutboxProperties properties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PreferenciasOutboxWorker(PendientePreferenciaRepository repository,
                                    EventoClient eventoClient,
                                    UsuarioClient usuarioClient,
                                    PreferenciasGateway gateway,
                                    OutboxProperties properties,
                                    CircuitBreakerRegistry circuitBreakerRegistry) {
        this.repository = repository;
        this.eventoClient = eventoClient;
        this.usuarioClient = usuarioClient;
        this.gateway = gateway;
        this.properties = properties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Async(AsyncConfig.PREFERENCIAS_TASK_EXECUTOR)
    public void procesar(PendientePreferencia pendiente) {
        String correlationId = pendiente.getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CorrelationContext.MDC_KEY, correlationId);
        try {
            CompraRegistradaRequest request = construirContrato(pendiente);
            gateway.registrar(request);
            repository.marcarEnviado(pendiente.getId(), Instant.now());
            log.info("preferencia_registrada idEntrada={} correlationId={}", pendiente.getIdEntrada(), correlationId);
        } catch (CallNotPermittedException exception) {
            reprogramarPorCircuitoAbierto(pendiente, correlationId);
        } catch (Exception exception) {
            registrarFallo(pendiente, exception, correlationId);
        } finally {
            MDC.clear();
        }
    }

    private CompraRegistradaRequest construirContrato(PendientePreferencia pendiente) {
        EventoResponse evento = eventoClient.obtenerPorId(pendiente.getIdEvento());
        UsuarioResponse usuario = usuarioClient.obtenerPorId(pendiente.getIdUsuario());
        if (evento == null || evento.tipo() == null || evento.tipo().isBlank()) {
            throw new IllegalStateException("El evento " + pendiente.getIdEvento() + " no tiene tipo");
        }
        if (usuario == null || usuario.ciudad() == null || usuario.ciudad().isBlank()) {
            throw new IllegalStateException("El usuario " + pendiente.getIdUsuario() + " no tiene ciudad");
        }
        List<ArtistaCompraRequest> artistas = evento.participantes() == null
                ? List.of()
                : evento.participantes().stream()
                        .filter(participante -> participante != null && participante.id() != null)
                        .map(participante -> new ArtistaCompraRequest(participante.id(), participante.nombre()))
                        .toList();
        return new CompraRegistradaRequest(
                pendiente.getIdUsuario(), pendiente.getIdEntrada(), pendiente.getIdEvento(),
                evento.tipo(), usuario.ciudad(), artistas);
    }

    private void registrarFallo(PendientePreferencia pendiente, Exception exception, String correlationId) {
        int nuevosIntentos = pendiente.getIntentos() + 1;
        Instant ahora = Instant.now();
        String error = resumirError(exception);
        if (nuevosIntentos >= properties.getIntentosMaximos()) {
            repository.guardarResultado(pendiente.getId(), PendientePreferencia.FALLIDO,
                    nuevosIntentos, null, error, ahora);
            log.warn("preferencia_fallida idEntrada={} correlationId={} intentos={}",
                    pendiente.getIdEntrada(), correlationId, nuevosIntentos);
            return;
        }
        Duration espera = properties.esperaDespuesDe(nuevosIntentos);
        repository.guardarResultado(pendiente.getId(), PendientePreferencia.PENDIENTE,
                nuevosIntentos, ahora.plus(espera), error, ahora);
        log.warn("preferencia_reintentable idEntrada={} correlationId={} intentos={} proximoEn={}",
                pendiente.getIdEntrada(), correlationId, nuevosIntentos, ahora.plus(espera));
    }

    private void reprogramarPorCircuitoAbierto(PendientePreferencia pendiente, String correlationId) {
        long milisEspera = circuitBreakerRegistry
                .circuitBreaker(ResilientPreferenciasGateway.NOMBRE_CIRCUITO)
                .getCircuitBreakerConfig()
                .getWaitIntervalFunctionInOpenState()
                .apply(1);
        Duration espera = Duration.ofMillis(milisEspera).plusSeconds(5);
        Instant ahora = Instant.now();
        repository.guardarResultado(pendiente.getId(), PendientePreferencia.PENDIENTE,
                pendiente.getIntentos(), ahora.plus(espera), "Circuito preferencias abierto", ahora);
        log.warn("preferencia_circuito_abierto idEntrada={} correlationId={} proximoEn={}",
                pendiente.getIdEntrada(), correlationId, ahora.plus(espera));
    }

    private String resumirError(Exception exception) {
        String mensaje = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        return mensaje.length() > 2000 ? mensaje.substring(0, 2000) : mensaje;
    }
}
