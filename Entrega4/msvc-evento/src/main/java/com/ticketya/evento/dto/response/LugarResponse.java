package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LugarResponse(
        @JsonAlias({"idLugar", "id_lugar"}) Long id,
        String nombre,
        String direccion,
        String ubicacion,
        String ciudad
) {
}
