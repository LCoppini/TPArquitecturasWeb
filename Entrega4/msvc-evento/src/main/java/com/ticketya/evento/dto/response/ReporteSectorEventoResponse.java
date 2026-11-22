package com.ticketya.evento.dto.response;

import java.math.BigDecimal;

/** Item público del reporte: cifras de msvc-entrada más capacidad y ocupación. */
public record ReporteSectorEventoResponse(
        Long idSector,
        String nombreSector,
        Integer capacidad,
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
