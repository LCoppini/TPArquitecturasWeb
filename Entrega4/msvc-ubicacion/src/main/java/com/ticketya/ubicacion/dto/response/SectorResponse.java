package com.ticketya.ubicacion.dto.response;

import com.ticketya.ubicacion.entity.TipoSector;

/**
 * Contrato consumido por msvc-evento (idSector, idLugar, nombre, capacidadMax) y
 * msvc-entrada (idSector, capacidadMax, filas, asientosPorFila, tipo). No incluye precio.
 */
public record SectorResponse(
        Long idSector,
        Long idLugar,
        String nombre,
        TipoSector tipo,
        Integer filas,
        Integer asientosPorFila,
        Integer capacidadMax
) {
}
