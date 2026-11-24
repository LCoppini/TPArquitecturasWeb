package com.ticketya.entrada.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompraResponse(
        Long idCompra,
        Long idEvento,
        String estado,
        BigDecimal total,
        Instant venceEn,
        List<EntradaResponse> entradas,
        String codigo,
        Integer cantidadDisponible
) {
}
