package com.ticketya.entrada.dto.response;

public record FilaEventoResponse(
        Long idEvento,
        String estado,
        Integer maxPersonas,
        Integer cupoEntradas
) {
}
