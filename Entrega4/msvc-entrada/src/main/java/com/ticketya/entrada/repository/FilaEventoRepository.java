package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.FilaEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FilaEventoRepository extends JpaRepository<FilaEvento, Long> {

    @Query(value = """
            select 1
            from (select pg_advisory_xact_lock(
                1346454345,
                cast(:idEvento as integer)
            )) as bloqueo
            """, nativeQuery = true)
    Integer bloquearEvento(@Param("idEvento") Long idEvento);

    @Query(value = """
            select 1
            from (select pg_advisory_xact_lock_shared(
                1346454345,
                cast(:idEvento as integer)
            )) as bloqueo
            """, nativeQuery = true)
    Integer bloquearEventoCompartido(@Param("idEvento") Long idEvento);

    @Query(value = """
            select 1
            from (select pg_advisory_xact_lock(hashtext(:clave))) as bloqueo
            """, nativeQuery = true)
    Integer bloquearClaveIdempotenciaCompra(@Param("clave") String clave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FilaEvento f where f.idEvento = :idEvento")
    Optional<FilaEvento> buscarPorEventoConBloqueo(@Param("idEvento") Long idEvento);
}
