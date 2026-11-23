package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record LugarResponse(
        @JsonAlias({"idLugar", "id_lugar"}) Long id,
        String nombre,
        String direccion,
        String ubicacion,
        String ciudad,
        List<SectorResponse> sectores
) {

    /** Constructor previo, conservado para no modificar los tests existentes. */
    public LugarResponse(Long id, String nombre, String direccion, String ubicacion, String ciudad) {
        this(id, nombre, direccion, ubicacion, ciudad, List.of());
    }
}
