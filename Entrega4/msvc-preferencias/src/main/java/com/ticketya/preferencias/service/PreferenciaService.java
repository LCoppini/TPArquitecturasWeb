package com.ticketya.preferencias.service;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.dto.response.CompraRegistradaResponse;
import com.ticketya.preferencias.dto.response.PreferenciaClienteResponse;

public interface PreferenciaService {

    CompraRegistradaResponse registrarCompra(CompraRegistradaRequest compra);

    PreferenciaClienteResponse obtener(Long idUsuario);
}
