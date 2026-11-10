package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record UsuarioResponse(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario,
        @JsonAlias({"roles", "usuarioRoles"}) List<String> roles
) {
}
