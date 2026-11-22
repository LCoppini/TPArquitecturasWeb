package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Participante del evento tal como lo expone msvc-evento; se trata como artista. */
public record ParticipanteResponse(
        @JsonAlias({"idParticipante", "id_participante"}) Long id,
        String nombre,
        String tipo
) {
}
