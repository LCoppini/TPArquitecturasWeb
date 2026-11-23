package com.ticketya.entrada.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Se publica cuando el reintegro de una devolucion queda concretado. {@code idCompra} es nulo en el flujo
 * de entrada individual (entradas sin Compra).
 */
public record DevolucionAprobadaEvent(
        Long idCompra,
        Long idEvento,
        Long idUsuario,
        List<Long> idsEntradas,
        BigDecimal monto,
        String correlationId
) {
}
