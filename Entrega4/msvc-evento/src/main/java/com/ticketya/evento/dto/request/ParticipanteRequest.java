package com.ticketya.evento.dto.request;

import jakarta.validation.constraints.Size;

public record ParticipanteRequest(
        Long id,
        @Size(max = 150) String nombre,
        @Size(max = 80) String tipo
) {
}
