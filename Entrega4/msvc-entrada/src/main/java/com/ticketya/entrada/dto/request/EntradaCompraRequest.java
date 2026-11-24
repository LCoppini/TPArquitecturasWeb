package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotNull;

public record EntradaCompraRequest(
        @NotNull Long idSector,
        Integer fila,
        Integer asiento
) {
}
