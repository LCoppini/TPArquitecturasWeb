package com.ticketya.entrada.dto.response;

import java.util.List;

/** Contrato interno consumido por msvc-evento; no expone capacidad ni nombre de sector. */
public record ReporteVentasEventoResponse(
        Long idEvento,
        List<ReporteSectorResponse> porSector,
        TotalesReporteResponse totales
) {
}
