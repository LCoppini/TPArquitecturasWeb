package com.ticketya.evento.service.impl;

import com.ticketya.evento.client.NotificacionesClient;
import com.ticketya.evento.config.AsyncConfig;
import com.ticketya.evento.dto.request.EventoPublicadoAvisoRequest;
import com.ticketya.evento.event.EventoPublicadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pide a msvc-notificaciones avisar a quienes siguen a los artistas del evento. Corre despues del commit
 * y un fallo solo queda en el log: no revierte la publicacion.
 */
@Component
public class EventoPublicadoListener {

    private static final Logger log = LoggerFactory.getLogger(EventoPublicadoListener.class);

    private final NotificacionesClient notificacionesClient;

    public EventoPublicadoListener(NotificacionesClient notificacionesClient) {
        this.notificacionesClient = notificacionesClient;
    }

    @Async(AsyncConfig.AVISOS_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void avisar(EventoPublicadoEvent evento) {
        try {
            notificacionesClient.eventoPublicado(new EventoPublicadoAvisoRequest(
                    evento.idEvento(), evento.evento(), evento.fechaHora(), evento.ciudad(),
                    evento.artistas().stream()
                            .map(a -> new EventoPublicadoAvisoRequest.ArtistaAviso(a.idArtista(), a.nombre()))
                            .toList()));
        } catch (RuntimeException exception) {
            log.warn("aviso_evento_publicado_fallido idEvento={}", evento.idEvento(), exception);
        }
    }
}
