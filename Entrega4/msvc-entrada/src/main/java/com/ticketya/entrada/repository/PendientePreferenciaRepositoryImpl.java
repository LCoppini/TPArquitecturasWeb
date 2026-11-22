package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.PendientePreferencia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implementación del claim atómico sobre PostgreSQL (ADR-0002: sin Testcontainers nuevos). */
@Repository
public class PendientePreferenciaRepositoryImpl implements PendientePreferenciaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Optional<PendientePreferencia> reclamarSiguiente(Instant ahora, Instant limiteStale) {
        List<?> ids = entityManager.createNativeQuery("""
                        UPDATE pendientes_preferencia p
                        SET estado = 'EN_PROCESO', actualizado_en = :ahora
                        WHERE p.id = (
                            SELECT c.id
                            FROM pendientes_preferencia c
                            WHERE (c.estado = 'PENDIENTE'
                                   AND (c.proximo_intento_en IS NULL OR c.proximo_intento_en <= :ahora))
                               OR (c.estado = 'EN_PROCESO' AND c.actualizado_en < :limiteStale)
                            ORDER BY COALESCE(c.proximo_intento_en, c.creado_en), c.id
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        RETURNING p.id
                        """)
                .setParameter("ahora", ahora)
                .setParameter("limiteStale", limiteStale)
                .getResultList();

        if (ids.isEmpty()) {
            return Optional.empty();
        }
        Long id = ((Number) ids.get(0)).longValue();
        return Optional.of(entityManager.find(PendientePreferencia.class, id));
    }
}
