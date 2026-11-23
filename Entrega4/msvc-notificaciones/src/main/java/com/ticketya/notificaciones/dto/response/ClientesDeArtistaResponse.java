package com.ticketya.notificaciones.dto.response;

import java.util.List;

/** Respuesta de msvc-preferencias: clientes con ese artista entre sus preferidos en la ciudad. */
public record ClientesDeArtistaResponse(Long idArtista, String ciudad, List<Long> idsUsuarios) {
}
