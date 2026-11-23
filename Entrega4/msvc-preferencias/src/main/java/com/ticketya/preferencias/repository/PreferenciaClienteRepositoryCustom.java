package com.ticketya.preferencias.repository;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;

import java.util.List;

public interface PreferenciaClienteRepositoryCustom {

    void registrarCompra(CompraRegistradaRequest compra);

    boolean existeCompra(Long idUsuario, Long idEntrada);

    /** Ids de los clientes con ese artista entre sus preferidos y esa ciudad (sin distinguir mayusculas ni acentos). */
    List<Long> buscarClientesPorArtistaYCiudad(Long idArtista, String ciudad);
}
