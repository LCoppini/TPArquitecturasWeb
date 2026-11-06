package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
