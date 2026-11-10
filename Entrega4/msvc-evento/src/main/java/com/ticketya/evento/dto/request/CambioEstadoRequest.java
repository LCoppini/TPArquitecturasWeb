package com.ticketya.evento.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambioEstadoRequest(@NotBlank String estado) {
}
