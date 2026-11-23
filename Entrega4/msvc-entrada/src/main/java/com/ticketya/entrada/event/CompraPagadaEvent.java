package com.ticketya.entrada.event;

import java.util.List;

public record CompraPagadaEvent(
        Long idCompra,
        Long idEvento,
        Long idUsuario,
        String correlationId,
        List<Long> idsEntradas
) {
}
