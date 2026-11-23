package com.ticketya.entrada.event;

import java.util.List;

/** Se publica al cancelar un evento; lleva a los usuarios que tenian entradas vigentes. */
public record EventoCanceladoEvent(
        Long idEvento,
        List<Long> idsUsuarios,
        String correlationId
) {
}
