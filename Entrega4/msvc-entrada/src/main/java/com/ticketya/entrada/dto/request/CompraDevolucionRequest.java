package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotNull;

public record CompraDevolucionRequest(@NotNull Boolean aprobarDevolucion) {
}
