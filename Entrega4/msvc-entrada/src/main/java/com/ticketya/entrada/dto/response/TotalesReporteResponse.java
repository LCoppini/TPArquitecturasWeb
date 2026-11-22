package com.ticketya.entrada.dto.response;

import java.math.BigDecimal;

/** Totales del evento, con los importes redondeados a dos decimales. */
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
