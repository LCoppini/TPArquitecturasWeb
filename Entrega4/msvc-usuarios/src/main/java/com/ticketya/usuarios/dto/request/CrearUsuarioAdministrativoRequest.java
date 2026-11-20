package com.ticketya.usuarios.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Alta administrativa: a diferencia del registro publico, SI permite elegir
 * roles privilegiados. Solo se expone bajo /api/admin/usuarios (ADR-0001).
 */
public record CrearUsuarioAdministrativoRequest(
        @NotNull @Positive Long dni,
        @NotBlank @Size(max = 255) String nombre,
        @NotBlank @Size(max = 255) String apellido,
        @NotBlank @Size(max = 255) String ciudad,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty Set<@NotBlank String> roles
) {
}
