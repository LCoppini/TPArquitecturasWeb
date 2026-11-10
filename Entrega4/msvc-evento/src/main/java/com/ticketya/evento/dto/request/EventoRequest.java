package com.ticketya.evento.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record EventoRequest(
        @NotNull Long idLugar,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotBlank @Size(max = 80) String tipo,
        @NotNull Instant fechaHora,
        List<@Valid ParticipanteRequest> participantes
) {
    public EventoRequest {
        participantes = participantes == null ? List.of() : new ArrayList<>(participantes);
    }
}
