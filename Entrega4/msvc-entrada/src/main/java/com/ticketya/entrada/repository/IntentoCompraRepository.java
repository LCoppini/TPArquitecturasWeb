package com.ticketya.entrada.repository;

import com.ticketya.entrada.entity.IntentoCompra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoCompraRepository extends JpaRepository<IntentoCompra, Long> {

    boolean existsByIdUsuarioAndIdTandaAndResultado(Long idUsuario, Long idTanda, String resultado);
}
