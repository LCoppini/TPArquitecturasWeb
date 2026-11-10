package com.ticketya.ubicacion.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edita solo los datos del lugar; los sectores no se modifican (sus ids deben ser estables). */
public record ActualizarLugarRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 250) String direccion,
        @NotBlank @Size(max = 100) String ciudad,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud
) {
}
