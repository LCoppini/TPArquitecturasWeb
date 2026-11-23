package com.ticketya.notificaciones.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Aviso interno de msvc-evento: un evento paso a PUBLICADO. */
public record EventoPublicadoRequest(
        @NotNull Long idEvento,
        @NotBlank @Size(max = 150) String evento,
        @NotBlank @Size(max = 60) String fechaHora,
        @NotBlank @Size(max = 120) String ciudad,
        @Schema(description = "Artistas participantes del evento")
        @NotEmpty List<@Valid ArtistaEvento> artistas
) {

    public record ArtistaEvento(@NotNull Long idArtista, @NotBlank @Size(max = 150) String nombre) {
    }
}
