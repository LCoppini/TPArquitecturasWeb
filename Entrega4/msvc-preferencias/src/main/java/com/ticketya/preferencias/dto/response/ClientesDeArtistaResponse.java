package com.ticketya.preferencias.dto.response;

import java.util.List;

/** Clientes con un artista entre sus preferidos y una ciudad dada. */
public record ClientesDeArtistaResponse(Long idArtista, String ciudad, List<Long> idsUsuarios) {
}
