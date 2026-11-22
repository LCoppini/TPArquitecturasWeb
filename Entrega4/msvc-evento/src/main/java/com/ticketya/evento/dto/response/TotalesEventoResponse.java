package com.ticketya.evento.dto.response;

import java.math.BigDecimal;

/** Totales públicos del reporte, con la ocupación calculada sobre entradas activas. */
public record TotalesEventoResponse(
        long entradasEmitidas,
        long entradasActivas,
        long entradasDevueltas,
        long enReintegro,
        BigDecimal porcentajeOcupacion,
        BigDecimal recaudacionBruta,
        BigDecimal recaudacionDevuelta,
        BigDecimal recaudacionNeta
) {
}
