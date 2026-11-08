package com.ticketya.usuarios.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotNull @Positive Long dni,
        @NotBlank @Size(max = 255) String nombre,
        @NotBlank @Size(max = 255) String apellido,
        @NotBlank @Size(max = 255) String ciudad
) {
}
