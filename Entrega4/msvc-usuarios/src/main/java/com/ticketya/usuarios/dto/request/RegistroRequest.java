package com.ticketya.usuarios.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Registro publico: no expone roles. El servicio fuerza CLIENTE para impedir
 * que el cliente se autoasigne ADMINISTRADOR, ORGANIZADOR o PERSONAL_ACCESO.
 */
public record RegistroRequest(
        @NotNull @Positive Long dni,
        @NotBlank @Size(max = 255) String nombre,
        @NotBlank @Size(max = 255) String apellido,
        @NotBlank @Size(max = 255) String ciudad,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
