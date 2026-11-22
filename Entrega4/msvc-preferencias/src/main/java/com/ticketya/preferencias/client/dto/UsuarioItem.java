package com.ticketya.preferencias.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UsuarioItem(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario,
        String ciudad
) {
}
