package com.ticketya.preferencias.repository;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;

public interface PreferenciaClienteRepositoryCustom {

    void registrarCompra(CompraRegistradaRequest compra);

    boolean existeCompra(Long idUsuario, Long idEntrada);
}
