package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    // Bloquea la entrada hasta finalizar la transacción para serializar pagos concurrentes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.id = :id")
    Optional<Entrada> buscarPorIdConBloqueo(@Param("id") Long id);

    @Query(value = """
            select 1
            from (select pg_advisory_xact_lock(hashtext(:clave))) as bloqueo
            """, nativeQuery = true)
    Integer bloquearReservasPorClave(@Param("clave") String clave);

    // Serializa reintentos de la misma clave para que devuelvan una única entrada.
    @Query(value = """
            select 1
            from (select pg_advisory_xact_lock(hashtext(:clave))) as bloqueo
            """, nativeQuery = true)
    Integer bloquearSolicitudIdempotente(@Param("clave") String clave);

    Optional<Entrada> findByIdUsuarioAndClaveIdempotencia(Long idUsuario, String claveIdempotencia);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    List<Entrada> findByIdUsuarioOrderByIdDesc(Long idUsuario);

    Optional<Entrada> findByIdAndIdUsuario(Long id, Long idUsuario);

    List<Entrada> findByIdEventoAndEstadoIn(Long idEvento, List<String> estados);

    @Query("""
            select count(e) from Entrada e
            where e.idEvento = :idEvento and e.idSector = :idSector
              and (
                e.estado in ('VALIDA', 'USADA')
                or (e.estado = 'RESERVADA' and e.expiraEn > :ahora)
              )
            """)
    long contarActivasDeSector(
            @Param("idEvento") Long idEvento,
            @Param("idSector") Long idSector,
            @Param("ahora") Instant ahora
    );

    // Solo reservas vigentes y entradas ya emitidas mantienen ocupado un asiento.
    @Query("""
            select count(e) from Entrada e
            where e.idEvento = :idEvento and e.idSector = :idSector
              and e.fila = :fila and e.asiento = :asiento
              and (
                e.estado in ('VALIDA', 'USADA')
                or (e.estado = 'RESERVADA' and e.expiraEn > :ahora)
              )
            """)
    long contarAsignacionesActivasDeAsiento(
            @Param("idEvento") Long idEvento,
            @Param("idSector") Long idSector,
            @Param("fila") Integer fila,
            @Param("asiento") Integer asiento,
            @Param("ahora") Instant ahora
    );

    @Query("""
            select count(e) from Entrada e
            where e.idEvento = :idEvento and e.idUsuario = :idUsuario
              and (
                e.estado in ('VALIDA', 'USADA')
                or (e.estado = 'RESERVADA' and e.expiraEn > :ahora)
              )
            """)
    long contarActivasDeUsuario(
            @Param("idEvento") Long idEvento,
            @Param("idUsuario") Long idUsuario,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            update Entrada e
            set e.estado = 'VENCIDA', e.expiraEn = null
            where e.estado = 'RESERVADA' and e.expiraEn <= :ahora
            """)
    int vencerReservas(@Param("ahora") Instant ahora);

    @Modifying
    @Query("""
            update Entrada e
            set e.estado = 'USADA'
            where e.codigoQr = :qr and e.idEvento = :idEvento and e.estado = 'VALIDA'
            """)
    int marcarUsada(@Param("qr") String qr, @Param("idEvento") Long idEvento);

    @Modifying
    @Query("""
            update Entrada e set e.estado = 'EN_REINTEGRO'
            where e.idEvento = :idEvento and e.estado in ('VALIDA', 'USADA')
            """)
    int marcarEntradasDeEventoEnReintegro(@Param("idEvento") Long idEvento);

}
