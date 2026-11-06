package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

public record EventoResponse(
        @JsonAlias({"idEvento", "id_evento"}) Long id,
        @JsonAlias({"fecha_hora", "fechaEvento", "fecha_evento"}) Instant fechaHora,
        String estado
) {
}
