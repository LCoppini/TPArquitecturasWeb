package com.ticketya.evento.dto.request;

import java.util.List;

/** Cuerpo de POST /internal/notificaciones/eventos-publicados (contrato de msvc-notificaciones). */
public record EventoPublicadoAvisoRequest(Long idEvento, String evento, String fechaHora, String ciudad,
                                          List<ArtistaAviso> artistas) {

    public record ArtistaAviso(Long idArtista, String nombre) {
    }
}
