package com.ticketya.ubicacion.dto.request;

import com.ticketya.ubicacion.entity.TipoSector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * NUMERADO: informar {@code filas} y {@code asientosPorFila} (la capacidad se calcula).
 * NO_NUMERADO: informar {@code capacidadMax} mayor a cero.
 */
public record SectorRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull TipoSector tipo,
        @Positive Integer filas,
        @Positive Integer asientosPorFila,
        @Positive Integer capacidadMax
) {
}
