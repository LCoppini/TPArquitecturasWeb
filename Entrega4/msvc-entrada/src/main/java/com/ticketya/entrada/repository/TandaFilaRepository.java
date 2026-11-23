package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.TandaFila;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface TandaFilaRepository extends JpaRepository<TandaFila, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from TandaFila t
            where t.idEvento = :idEvento and t.estado = 'ABIERTA'
            """)
    Optional<TandaFila> buscarAbiertaPorEventoConBloqueo(@Param("idEvento") Long idEvento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TandaFila t where t.id = :id")
    Optional<TandaFila> buscarPorIdConBloqueo(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update tanda_fila
            set cupo_restante = cupo_restante - :cantidad
            where id = :id and estado = 'ABIERTA' and cupo_restante >= :cantidad
            """, nativeQuery = true)
    int descontarCupo(@Param("id") Long id, @Param("cantidad") int cantidad);

    Optional<TandaFila> findFirstByIdEventoOrderByNumeroDesc(Long idEvento);

    @Query("""
            select t.idEvento from TandaFila t
            where t.estado = 'ABIERTA' and t.venceEn <= :ahora
            """)
    List<Long> buscarEventosConTandaVencida(@Param("ahora") java.time.Instant ahora);
}
