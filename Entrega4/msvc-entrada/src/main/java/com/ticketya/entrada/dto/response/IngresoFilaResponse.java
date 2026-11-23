package com.ticketya.entrada.dto.response;

import java.time.Instant;

public record IngresoFilaResponse(
        Long idEvento,
        Long idIngreso,
        Long secuencia,
        String estado,
        Long posicion,
        Instant venceEn,
        Long idTanda
) {
}
