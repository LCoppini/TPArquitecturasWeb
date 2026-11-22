package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.CompraRegistradaRequest;

/** Llamada a preferencias con circuito breaker y time limit, sin reintentos internos. */
public interface PreferenciasGateway {

    void registrar(CompraRegistradaRequest compra);
}
