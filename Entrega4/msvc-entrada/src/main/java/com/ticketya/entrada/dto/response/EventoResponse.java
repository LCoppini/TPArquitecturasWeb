package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record EventoResponse(
        @JsonAlias({"idEvento", "id_evento"}) Long id,
        @JsonAlias({"fecha_hora", "fechaEvento", "fecha_evento"}) Instant fechaHora,
        String estado,
        String tipo,
        List<ParticipanteResponse> participantes
) {
    /** Constructor previo, conservado para no modificar los tests existentes. */
    public EventoResponse(Long id, Instant fechaHora, String estado) {
        this(id, fechaHora, estado, null, List.of());
    }
}
