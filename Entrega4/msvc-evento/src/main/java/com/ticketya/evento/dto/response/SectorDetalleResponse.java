package com.ticketya.evento.dto.response;

import java.math.BigDecimal;

/** Sector de un evento con su precio y sus dimensiones (filas y asientos solo si es numerado). */
public record SectorDetalleResponse(
        Long idSector,
        String nombre,
        String tipo,
        Integer capacidad,
        Integer filas,
        Integer asientosPorFila,
        BigDecimal precio
) {
}
