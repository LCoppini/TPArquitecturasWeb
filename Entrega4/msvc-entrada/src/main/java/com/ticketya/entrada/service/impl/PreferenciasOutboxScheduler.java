package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.config.OutboxProperties;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Fase 1 del outbox: reclamo atómico y despacho asíncrono, sin transacción propia. */
@Component
public class PreferenciasOutboxScheduler {

    private final PendientePreferenciaRepository repository;
    private final OutboxProperties properties;
    private final PreferenciasOutboxWorker worker;

    public PreferenciasOutboxScheduler(PendientePreferenciaRepository repository,
                                       OutboxProperties properties,
                                       PreferenciasOutboxWorker worker) {
        this.repository = repository;
        this.properties = properties;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${ticketya.preferencias.outbox.poll-ms:5s}")
    public void reclamarYDespachar() {
        Instant ahora = Instant.now();
        repository.reclamarSiguiente(ahora, ahora.minus(properties.getStaleTimeout()))
                .ifPresent(worker::procesar);
    }
}
