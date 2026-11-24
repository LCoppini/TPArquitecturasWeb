package com.ticketya.usuarios.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCiudadRequest(@NotBlank @Size(max = 255) String ciudad) {
}
