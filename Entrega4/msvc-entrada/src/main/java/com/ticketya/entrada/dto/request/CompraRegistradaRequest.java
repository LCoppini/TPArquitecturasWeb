package com.ticketya.entrada.dto.request;

import java.util.List;

/** Contrato interno POST /internal/preferencias/compras; idEntrada es la idempotencia. */
public record CompraRegistradaRequest(
        Long idUsuario,
        Long idEntrada,
        Long idEvento,
        String tipoEvento,
        String ciudad,
        List<ArtistaCompraRequest> artistas
) {
}
