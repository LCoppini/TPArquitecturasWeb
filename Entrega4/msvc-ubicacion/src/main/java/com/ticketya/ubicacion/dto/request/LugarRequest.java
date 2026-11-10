package com.ticketya.ubicacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record LugarRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 250) String direccion,
        @NotBlank @Size(max = 100) String ciudad,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
        @NotEmpty List<@Valid @NotNull SectorRequest> sectores
) {
    public LugarRequest {
        sectores = sectores == null ? List.of() : new ArrayList<>(sectores);
    }
}
