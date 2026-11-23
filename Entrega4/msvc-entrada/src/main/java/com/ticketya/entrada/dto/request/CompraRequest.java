package com.ticketya.entrada.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompraRequest(
        @NotNull Long idEvento,
        @NotEmpty @Size(max = 6) List<@Valid EntradaCompraRequest> entradas
) {
}
