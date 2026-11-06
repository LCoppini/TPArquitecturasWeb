package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UsuarioResponse(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario
) {
}
