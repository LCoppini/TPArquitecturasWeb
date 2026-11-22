package com.ticketya.entrada.dto.request;

/** Artista (participante del evento) tal como lo espera el contrato interno de preferencias. */
public record ArtistaCompraRequest(Long idArtista, String nombre) {
}
