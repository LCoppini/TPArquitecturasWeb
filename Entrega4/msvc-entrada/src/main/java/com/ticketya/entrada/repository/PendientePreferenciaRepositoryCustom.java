package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.PendientePreferencia;

import java.time.Instant;
import java.util.Optional;

/** Operaciones atómicas del outbox; la implementación usa SQL de PostgreSQL. */
public interface PendientePreferenciaRepositoryCustom {

    /**
     * Toma el registro elegible más viejo y lo pasa a EN_PROCESO en una sola
     * sentencia. SKIP LOCKED permite que dos instancias reclamen filas distintas.
     */
    Optional<PendientePreferencia> reclamarSiguiente(Instant ahora, Instant limiteStale);
}
