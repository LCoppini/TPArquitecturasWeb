package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambios de un evento, ya descritos en texto por msvc-evento (ej. "fecha: 10/01/2027 21:00 a 12/01/2027 21:00"). */
public record EventoModificadoRequest(@NotBlank @Size(max = 500) String cambios) {
}
