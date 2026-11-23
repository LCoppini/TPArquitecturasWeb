package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.DevolucionAprobadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/** Avisa por correo que la devolucion fue aprobada y reintegrada. Un fallo no afecta la devolucion. */
@Component
public class DevolucionNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(DevolucionNotificacionListener.class);

    private final NotificacionesClient notificacionesClient;

    public DevolucionNotificacionListener(NotificacionesClient notificacionesClient) {
        this.notificacionesClient = notificacionesClient;
    }

    @Async(AsyncConfig.NOTIFICACIONES_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(DevolucionAprobadaEvent evento) {
        MDC.put(CorrelationContext.MDC_KEY, evento.correlationId());
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("idEvento", evento.idEvento());
            datos.put("idsEntradas", evento.idsEntradas());
            datos.put("monto", evento.monto());
            if (evento.idCompra() != null) {
                datos.put("idCompra", evento.idCompra());
            }
            notificacionesClient.enviar(new NotificacionRequest(
                    "DEVOLUCION_APROBADA", evento.idUsuario(), evento.correlationId(), datos));
        } catch (RuntimeException exception) {
            log.warn("notificacion_fallida tipo=DEVOLUCION_APROBADA idCompra={} idEvento={} correlationId={}",
                    evento.idCompra(), evento.idEvento(), evento.correlationId(), exception);
        } finally {
            MDC.remove(CorrelationContext.MDC_KEY);
        }
    }
}
