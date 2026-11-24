package com.ticketya.evento.service.impl;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.config.AsyncConfig;
import com.ticketya.evento.dto.request.ModificacionEventoRequest;
import com.ticketya.evento.event.EventoModificadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa a msvc-entrada, que conoce a los compradores, de que el evento cambio. Se ejecuta despues del commit
 * y un fallo solo queda en el log: no revierte la edicion.
 */
@Component
public class EventoModificadoListener {

    private static final Logger log = LoggerFactory.getLogger(EventoModificadoListener.class);

    private final EntradaClient entradaClient;

    public EventoModificadoListener(EntradaClient entradaClient) {
        this.entradaClient = entradaClient;
    }

    @Async(AsyncConfig.AVISOS_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(EventoModificadoEvent evento) {
        try {
            entradaClient.notificarModificacion(evento.idEvento(), new ModificacionEventoRequest(evento.cambios()));
        } catch (RuntimeException exception) {
            log.warn("aviso_modificacion_fallido idEvento={}", evento.idEvento(), exception);
        }
    }
}
