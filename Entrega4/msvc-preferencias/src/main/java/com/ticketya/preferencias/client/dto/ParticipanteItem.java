package com.ticketya.preferencias.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ParticipanteItem(
        @JsonAlias({"idParticipante", "id_participante"}) Long id,
        String nombre,
        String tipo
) {
}
