package com.ticketya.evento.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EventoSectorRequest(
        @NotNull @Positive Long idSector,
        @NotNull @DecimalMin("0.0") BigDecimal precio
) {
}
