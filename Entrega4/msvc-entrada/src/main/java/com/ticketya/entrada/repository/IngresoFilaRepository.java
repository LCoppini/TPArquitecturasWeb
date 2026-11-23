package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.IngresoFila;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngresoFilaRepository extends JpaRepository<IngresoFila, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from IngresoFila i
            where i.idEvento = :idEvento and i.idUsuario = :idUsuario
              and i.estado in ('EN_ESPERA', 'HABILITADO')
            order by i.secuencia desc
            """)
    List<IngresoFila> buscarVigentesConBloqueo(
            @Param("idEvento") Long idEvento,
            @Param("idUsuario") Long idUsuario
    );

    Optional<IngresoFila> findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(Long idEvento, Long idUsuario);

    Optional<IngresoFila> findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(
            Long idEvento,
            Long idUsuario,
            String estado
    );

    List<IngresoFila> findByIdEventoAndEstadoOrderBySecuenciaAsc(Long idEvento, String estado);

    List<IngresoFila> findByIdEventoAndEstadoOrderBySecuenciaAsc(
            Long idEvento,
            String estado,
            Pageable pageable
    );

    boolean existsByIdEventoAndEstado(Long idEvento, String estado);

    List<IngresoFila> findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(
            Long idEvento,
            Long idTanda,
            String estado
    );

    long countByIdEventoAndEstadoAndSecuenciaLessThan(Long idEvento, String estado, Long secuencia);

    @Query("""
            select i from IngresoFila i
            where i.idEvento = :idEvento and i.idTanda = :idTanda
              and i.estado = 'HABILITADO'
            order by i.secuencia asc
            """)
    List<IngresoFila> buscarHabilitadosDeTanda(
            @Param("idEvento") Long idEvento,
            @Param("idTanda") Long idTanda
    );

    @Query("""
            select count(i) from IngresoFila i
            where i.idEvento = :idEvento and i.idTanda = :idTanda
            """)
    long contarAsignadosDeTanda(
            @Param("idEvento") Long idEvento,
            @Param("idTanda") Long idTanda
    );
}
