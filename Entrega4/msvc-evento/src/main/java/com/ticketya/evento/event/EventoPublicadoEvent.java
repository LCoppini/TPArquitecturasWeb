package com.ticketya.evento.event;

import java.util.List;

/** Un evento paso a PUBLICADO; se publica dentro de la transaccion y se atiende despues del commit. */
public record EventoPublicadoEvent(Long idEvento, String evento, String fechaHora, String ciudad,
                                   List<Artista> artistas) {

    public record Artista(Long idArtista, String nombre) {
    }
}
