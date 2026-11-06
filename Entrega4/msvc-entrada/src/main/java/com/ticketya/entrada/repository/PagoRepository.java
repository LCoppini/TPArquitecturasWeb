package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdEntradaAndClaveIdempotencia(Long idEntrada, String claveIdempotencia);

    Optional<Pago> findFirstByIdEntradaOrderByIdPagoDesc(Long idEntrada);

    List<Pago> findByIdUsuarioOrderByIdPagoDesc(Long idUsuario);

    Optional<Pago> findByIdPagoAndIdUsuario(Long idPago, Long idUsuario);

    List<Pago> findByIdEntradaIn(List<Long> idsEntrada);

}
