package com.ticketya.usuarios.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record ActualizarRolesRequest(@NotNull Set<@NotBlank String> roles) {
}
