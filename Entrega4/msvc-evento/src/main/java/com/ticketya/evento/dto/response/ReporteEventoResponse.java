package com.ticketya.evento.dto.response;

import java.util.List;

/** Respuesta pública del reporte del organizador dueño del evento. */
public record ReporteEventoResponse(
        Long idEvento,
        String estadoEvento,
        List<ReporteSectorEventoResponse> porSector,
        TotalesEventoResponse totales
) {
}
