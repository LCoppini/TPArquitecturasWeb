package com.ticketya.evento.dto.response;

import java.time.Instant;
import java.util.List;

/** Vista de un evento para el cliente: incluye lugar y sectores resueltos desde msvc-ubicacion. */
public record EventoDetalleResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipo,
        Instant fechaHora,
        String estado,
        LugarDetalleResponse lugar,
        List<ParticipanteResponse> participantes,
        List<SectorDetalleResponse> sectores
) {
}
