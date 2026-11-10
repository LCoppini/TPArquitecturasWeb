package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record EventoResponse(
        @JsonAlias({"idEvento", "id_evento"}) Long id,
        Long idLugar,
        Long idOrganizador,
        String nombre,
        String descripcion,
        String tipo,
        @JsonAlias({"fecha_hora", "fechaEvento", "fecha_evento"}) Instant fechaHora,
        String estado,
        List<ParticipanteResponse> participantes,
        List<EventoSectorResponse> sectores
) {
}
