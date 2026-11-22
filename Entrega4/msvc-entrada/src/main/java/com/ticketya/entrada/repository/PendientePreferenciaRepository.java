package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.PendientePreferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface PendientePreferenciaRepository extends JpaRepository<PendientePreferencia, Long>,
        PendientePreferenciaRepositoryCustom {

    Optional<PendientePreferencia> findByIdEntrada(Long idEntrada);

    /** Actualiza resultado, fecha e intentos juntos, solo si la fila sigue reclamada por esta instancia. */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE pendientes_preferencia
            SET estado = :estado,
                intentos = :intentos,
                proximo_intento_en = :proximoIntentoEn,
                ultimo_error = :ultimoError,
                actualizado_en = :ahora
            WHERE id = :id AND estado = 'EN_PROCESO'
            """, nativeQuery = true)
    int guardarResultado(@Param("id") Long id,
                         @Param("estado") String estado,
                         @Param("intentos") int intentos,
                         @Param("proximoIntentoEn") Instant proximoIntentoEn,
                         @Param("ultimoError") String ultimoError,
                         @Param("ahora") Instant ahora);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE pendientes_preferencia
            SET estado = 'ENVIADO', ultimo_error = NULL, actualizado_en = :ahora
            WHERE id = :id AND estado = 'EN_PROCESO'
            """, nativeQuery = true)
    int marcarEnviado(@Param("id") Long id, @Param("ahora") Instant ahora);
}
