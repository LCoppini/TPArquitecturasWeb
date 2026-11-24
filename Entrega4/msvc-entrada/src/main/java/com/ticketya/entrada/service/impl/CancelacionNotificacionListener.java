package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.EventoCanceladoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/** Avisa por correo a quienes tenian entradas de un evento que se cancelo. Un fallo no afecta la cancelacion. */
@Component
public class CancelacionNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(CancelacionNotificacionListener.class);

    private final NotificacionesClient notificacionesClient;

    public CancelacionNotificacionListener(NotificacionesClient notificacionesClient) {
        this.notificacionesClient = notificacionesClient;
    }

    @Async(AsyncConfig.NOTIFICACIONES_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(EventoCanceladoEvent evento) {
        MDC.put(CorrelationContext.MDC_KEY, evento.correlationId());
        try {
            for (Long idUsuario : evento.idsUsuarios()) {
                try {
                    notificacionesClient.enviar(new NotificacionRequest(
                            "EVENTO_CANCELADO", idUsuario, evento.correlationId(),
                            Map.of("idEvento", evento.idEvento())));
                } catch (RuntimeException exception) {
                    log.warn("notificacion_fallida tipo=EVENTO_CANCELADO idEvento={} idUsuario={} correlationId={}",
                            evento.idEvento(), idUsuario, evento.correlationId(), exception);
                }
            }
        } finally {
            MDC.remove(CorrelationContext.MDC_KEY);
        }
    }
}
