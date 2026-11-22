package com.ticketya.evento.dto.response;

import java.math.BigDecimal;

/** Totales agregados en el contrato interno. */
public record TotalesReporteResponse(
        long entradasEmitidas,
        long entradasActivas,
        long entradasDevueltas,
        long enReintegro,
        BigDecimal recaudacionBruta,
        BigDecimal recaudacionDevuelta,
        BigDecimal recaudacionNeta
) {
}
