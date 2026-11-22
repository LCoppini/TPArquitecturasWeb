package com.ticketya.entrada.dto.response;

import java.math.BigDecimal;

/** Cifras de un sector del evento para el contrato interno del reporte. */
public record ReporteSectorResponse(
        Long idSector,
        long entradasEmitidas,
        long entradasActivas,
        long entradasDevueltas,
        long enReintegro,
        BigDecimal recaudacionBruta,
        BigDecimal recaudacionDevuelta,
        BigDecimal recaudacionNeta
) {
}
