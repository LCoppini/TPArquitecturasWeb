package com.ticketya.entrada.event;

import java.util.List;

/** Se publica cuando msvc-evento informa un cambio de fecha, lugar o nombre; lleva a los usuarios con entradas vigentes. */
public record EventoModificadoEvent(
        Long idEvento,
        List<Long> idsUsuarios,
        String cambios,
        String correlationId
) {
}
