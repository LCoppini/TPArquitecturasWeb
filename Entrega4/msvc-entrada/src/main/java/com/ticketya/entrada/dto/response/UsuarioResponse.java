package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UsuarioResponse(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario,
        String ciudad
) {
    /** Constructor previo, conservado para no modificar los tests existentes. */
    public UsuarioResponse(Long idUsuario) {
        this(idUsuario, null);
    }
}
