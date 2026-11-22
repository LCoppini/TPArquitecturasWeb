package com.ticketya.preferencias.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArtistaCompraRequest(
        @NotNull Long idArtista,
        @NotBlank String nombre
) {
}
