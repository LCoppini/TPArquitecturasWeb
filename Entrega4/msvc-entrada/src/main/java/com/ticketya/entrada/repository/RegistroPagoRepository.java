package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RegistroPagoRepository extends JpaRepository<RegistroPago, RegistroPagoId> {

    @Query("select r from RegistroPago r where r.entrada.idUsuario = :idUsuario order by r.fechaPago desc")
    java.util.List<RegistroPago> findByUsuario(@Param("idUsuario") Long idUsuario);

    @Query("""
            select r from RegistroPago r
            where r.id.idEntrada = :idEntrada and r.id.idPago = :idPago
              and r.entrada.idUsuario = :idUsuario
            """)
    java.util.Optional<RegistroPago> findByIdAndUsuario(
            @Param("idEntrada") Long idEntrada,
            @Param("idPago") Long idPago,
            @Param("idUsuario") Long idUsuario
    );

    /** Proyección del reporte: un renglón por (sector, estado del pago). */
    interface RecaudacionPorSectorYEstadoPago {
        Long getIdSector();

        String getEstadoPago();

        BigDecimal getMonto();
    }

    @Query("""
            select r.entrada.idSector as idSector,
                   r.pago.estado as estadoPago,
                   sum(r.montoTotal) as monto
            from RegistroPago r
            where r.entrada.idEvento = :idEvento and r.pago.estado in :estados
            group by r.entrada.idSector, r.pago.estado
            """)
    List<RecaudacionPorSectorYEstadoPago> recaudarPorSectorYEstadoPago(
            @Param("idEvento") Long idEvento,
            @Param("estados") List<String> estados
    );
}
