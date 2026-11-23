package com.ticketya.entrada.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FilaEventoRepositoryLockQueryTest {

    @Test
    void advisoryLocksUsanNamespaceFijoYParametroEventoTipado() throws Exception {
        Query exclusivo = FilaEventoRepository.class
                .getMethod("bloquearEvento", Long.class)
                .getAnnotation(Query.class);
        Query compartido = FilaEventoRepository.class
                .getMethod("bloquearEventoCompartido", Long.class)
                .getAnnotation(Query.class);

        assertTrue(exclusivo.value().contains("pg_advisory_xact_lock("));
        assertTrue(compartido.value().contains("pg_advisory_xact_lock_shared("));
        assertTrue(exclusivo.value().contains("1346454345"));
        assertTrue(compartido.value().contains("1346454345"));
        assertTrue(exclusivo.value().contains("cast(:idEvento as integer)"));
        assertTrue(compartido.value().contains("cast(:idEvento as integer)"));
        assertTrue(exclusivo.nativeQuery());
        assertTrue(compartido.nativeQuery());
    }
}
