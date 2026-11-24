package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record EventoResponse(
        @JsonAlias({"idEvento", "id_evento"}) Long id,
        Long idLugar,
        @JsonAlias({"id_organizador"}) Long idOrganizador,
        String nombre,
        String descripcion,
        @JsonAlias({"fecha_hora", "fechaEvento", "fecha_evento"}) Instant fechaHora,
        String estado,
        String tipo,
        List<ParticipanteResponse> participantes,
        List<EventoSectorResponse> sectores
) {
    /** Constructor previo, conservado para no modificar los tests existentes. */
    public EventoResponse(Long id, Instant fechaHora, String estado) {
        this(id, null, null, null, null, fechaHora, estado, null, List.of(), List.of());
    }

    /** Constructor previo, conservado para contratos existentes del cliente Feign. */
    public EventoResponse(
            Long id,
            Instant fechaHora,
            String estado,
            String tipo,
            List<ParticipanteResponse> participantes
    ) {
        this(id, null, null, null, null, fechaHora, estado, tipo, participantes, List.of());
    }
}
