package com.ticketya.notificaciones.repository;

import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByClaveIdempotencia(String claveIdempotencia);

    List<Notificacion> findTop100ByOrderByIdDesc();

    List<Notificacion> findTop100ByIdUsuarioOrderByIdDesc(Long idUsuario);

    List<Notificacion> findTop100ByEstadoOrderByIdDesc(EstadoNotificacion estado);

    List<Notificacion> findTop100ByIdUsuarioAndEstadoOrderByIdDesc(Long idUsuario, EstadoNotificacion estado);

    /**
     * Reclamo atomico: pasa la notificacion a ENVIANDO solo si sigue elegible. Devuelve 1 si la reclamo
     * quien llama y 0 si otro hilo o instancia se adelanto (o ya no corresponde enviarla).
     */
    @Transactional
    @Modifying
    @Query("""
            update Notificacion n
               set n.estado = :enviando, n.fechaActualizacion = :ahora
             where n.id = :id
               and ((n.estado in :reintentables and n.intentos < :maximo)
                    or (n.estado = :enviando and n.fechaActualizacion < :limiteStale))
            """)
    int reclamar(@Param("id") Long id,
                 @Param("enviando") EstadoNotificacion enviando,
                 @Param("reintentables") Collection<EstadoNotificacion> reintentables,
                 @Param("maximo") int maximo,
                 @Param("ahora") Instant ahora,
                 @Param("limiteStale") Instant limiteStale);

    /** Ids que el worker debe intentar: reintentos vencidos y notificaciones ENVIANDO trabadas. */
    @Query("""
            select n.id from Notificacion n
             where (n.estado in :reintentables and n.intentos < :maximo
                    and (n.proximoIntentoEn is null or n.proximoIntentoEn <= :ahora))
                or (n.estado = :enviando and n.fechaActualizacion < :limiteStale)
             order by n.fechaCreacion asc
            """)
    List<Long> buscarElegibles(@Param("reintentables") Collection<EstadoNotificacion> reintentables,
                               @Param("enviando") EstadoNotificacion enviando,
                               @Param("maximo") int maximo,
                               @Param("ahora") Instant ahora,
                               @Param("limiteStale") Instant limiteStale,
                               Pageable pageable);
}
