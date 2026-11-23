package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.NotificacionesClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.event.EventoModificadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/** Avisa por correo a quienes tienen entradas de un evento que cambio. Un fallo no afecta a los demas envios. */
@Component
public class ModificacionNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(ModificacionNotificacionListener.class);

    private final NotificacionesClient notificacionesClient;

    public ModificacionNotificacionListener(NotificacionesClient notificacionesClient) {
        this.notificacionesClient = notificacionesClient;
    }

    @Async(AsyncConfig.NOTIFICACIONES_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(EventoModificadoEvent evento) {
        MDC.put(CorrelationContext.MDC_KEY, evento.correlationId());
        try {
            for (Long idUsuario : evento.idsUsuarios()) {
                try {
                    notificacionesClient.enviar(new NotificacionRequest(
                            "EVENTO_MODIFICADO", idUsuario, evento.correlationId(),
                            Map.of("idEvento", evento.idEvento(), "cambios", evento.cambios())));
                } catch (RuntimeException exception) {
                    log.warn("notificacion_fallida tipo=EVENTO_MODIFICADO idEvento={} idUsuario={} correlationId={}",
                            evento.idEvento(), idUsuario, evento.correlationId(), exception);
                }
            }
        } finally {
            MDC.remove(CorrelationContext.MDC_KEY);
        }
    }
}
