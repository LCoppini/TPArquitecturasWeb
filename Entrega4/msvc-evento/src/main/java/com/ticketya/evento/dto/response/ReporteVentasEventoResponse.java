package com.ticketya.evento.dto.response;

import java.util.List;

/** Espejo del contrato interno de msvc-entrada; no se expone al público. */
public record ReporteVentasEventoResponse(
        Long idEvento,
        List<ReporteSectorResponse> porSector,
        TotalesReporteResponse totales
) {
}
