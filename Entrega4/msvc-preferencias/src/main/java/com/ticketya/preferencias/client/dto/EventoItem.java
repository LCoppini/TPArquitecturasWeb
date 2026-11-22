package com.ticketya.preferencias.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record EventoItem(
        @JsonAlias({"idEvento", "id_evento"}) Long id,
        String tipo,
        @JsonAlias({"fecha_hora", "fechaEvento", "fecha_evento"}) Instant fechaHora,
        String estado,
        List<ParticipanteItem> participantes
) {
}
