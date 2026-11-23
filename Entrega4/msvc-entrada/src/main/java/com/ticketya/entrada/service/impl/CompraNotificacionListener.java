package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.CompraPagadaEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class CompraNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(CompraNotificacionListener.class);
    private static final String CIRCUIT_BREAKER = "notificaciones";

    private final NotificacionesClient notificacionesClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CompraNotificacionListener(
            NotificacionesClient notificacionesClient,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.notificacionesClient = notificacionesClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Async(AsyncConfig.NOTIFICACIONES_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(CompraPagadaEvent evento) {
        MDC.put(CorrelationContext.MDC_KEY, evento.correlationId());
        try {
            ejecutar(evento, new NotificacionRequest(
                    "COMPRA_CONFIRMADA",
                    evento.idUsuario(),
                    evento.correlationId(),
                    Map.of("idCompra", evento.idCompra(), "idEvento", evento.idEvento(),
                            "idsEntradas", evento.idsEntradas())
            ));
            for (Long idEntrada : evento.idsEntradas()) {
                ejecutar(evento, new NotificacionRequest(
                        "ENTRADA_EMITIDA",
                        evento.idUsuario(),
                        evento.correlationId(),
                        Map.of("idCompra", evento.idCompra(), "idEvento", evento.idEvento(),
                                "idEntrada", idEntrada)
                ));
            }
        } finally {
            MDC.remove(CorrelationContext.MDC_KEY);
        }
    }

    private void ejecutar(CompraPagadaEvent evento, NotificacionRequest request) {
        try {
            circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER)
                    .executeRunnable(() -> notificacionesClient.enviar(request));
        } catch (CallNotPermittedException exception) {
            log.warn("notificacion_circuito_abierto tipo={} idCompra={} correlationId={}",
                    request.tipo(), evento.idCompra(), evento.correlationId());
        } catch (RuntimeException exception) {
            log.warn("notificacion_fallida tipo={} idCompra={} correlationId={}",
                    request.tipo(), evento.idCompra(), evento.correlationId(), exception);
        }
    }
}
