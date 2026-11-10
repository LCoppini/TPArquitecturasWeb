package com.ticketya.evento.dto.response;

public record DisponibilidadResponse(
        Long idEvento,
        Long idSector,
        Integer capacidad,
        Long ocupadas,
        Long disponibles
) {
}
