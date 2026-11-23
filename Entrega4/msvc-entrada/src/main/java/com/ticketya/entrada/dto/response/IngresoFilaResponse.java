package com.ticketya.entrada.dto.response;

import java.time.Instant;

public record IngresoFilaResponse(
        Long idEvento,
        Long idIngreso,
        Long secuencia,
        String estado,
        Long posicion,
        Instant venceEn,
        Long idTanda,
        Long esperaEstimadaSegundos
) {

    /** Constructor previo, conservado para no modificar los tests existentes. */
    public IngresoFilaResponse(
            Long idEvento,
            Long idIngreso,
            Long secuencia,
            String estado,
            Long posicion,
            Instant venceEn,
            Long idTanda
    ) {
        this(idEvento, idIngreso, secuencia, estado, posicion, venceEn, idTanda, null);
    }
}
