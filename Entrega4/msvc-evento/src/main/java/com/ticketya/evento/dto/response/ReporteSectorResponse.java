package com.ticketya.evento.dto.response;

import java.math.BigDecimal;

/** Cifras agregadas de un sector en el contrato interno. */
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
