package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.Compra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByIdUsuarioAndClaveIdempotencia(Long idUsuario, String claveIdempotencia);

    Optional<Compra> findByIdCompraAndIdUsuario(Long idCompra, Long idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Compra c
            where c.idUsuario = :idUsuario and c.idEvento = :idEvento and c.estado = 'RESERVADA'
            order by c.idCompra
            """)
    List<Compra> buscarReservadasDeUsuarioConBloqueo(
            @Param("idUsuario") Long idUsuario,
            @Param("idEvento") Long idEvento
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.idCompra = :idCompra")
    Optional<Compra> buscarPorIdConBloqueo(@Param("idCompra") Long idCompra);

    @Query("""
            select c.idEvento from Compra c
            where c.estado = 'RESERVADA' and c.venceEn <= :ahora
            """)
    List<Long> buscarEventosConCompraVencida(@Param("ahora") Instant ahora);

    List<Compra> findByIdEventoAndEstado(Long idEvento, String estado);

    boolean existsByIdEventoAndIdTandaAndEstado(Long idEvento, Long idTanda, String estado);

    boolean existsByIdEventoAndEstado(Long idEvento, String estado);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update compras set estado = 'PAGADA'
            where id_compra = :idCompra and estado = 'RESERVADA' and vence_en > :ahora
            """, nativeQuery = true)
    int confirmarSiVigente(@Param("idCompra") Long idCompra, @Param("ahora") Instant ahora);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update compras set estado = 'RECHAZADA'
            where id_compra = :idCompra and estado = 'RESERVADA' and vence_en > :ahora
            """, nativeQuery = true)
    int rechazarSiVigente(@Param("idCompra") Long idCompra, @Param("ahora") Instant ahora);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update compras set estado = 'VENCIDA'
            where id_compra = :idCompra and estado = 'RESERVADA' and vence_en <= :ahora
            """, nativeQuery = true)
    int vencerSiVencida(@Param("idCompra") Long idCompra, @Param("ahora") Instant ahora);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update compras set estado = 'CANCELADA'
            where id_compra = :idCompra and estado = 'RESERVADA'
            """, nativeQuery = true)
    int cancelarSiReservada(@Param("idCompra") Long idCompra);
}
