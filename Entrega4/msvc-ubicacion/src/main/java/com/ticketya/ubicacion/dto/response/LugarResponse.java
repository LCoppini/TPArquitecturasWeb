package com.ticketya.ubicacion.dto.response;

import java.util.List;

/**
 * Contrato consumido por msvc-evento: id, nombre, direccion, ciudad y ubicacion ("latitud,longitud").
 * Se agregan latitud, longitud y sectores como datos adicionales.
 */
public record LugarResponse(
        Long id,
        String nombre,
        String direccion,
        String ciudad,
        String ubicacion,
        Double latitud,
        Double longitud,
        List<SectorResponse> sectores
) {
}
