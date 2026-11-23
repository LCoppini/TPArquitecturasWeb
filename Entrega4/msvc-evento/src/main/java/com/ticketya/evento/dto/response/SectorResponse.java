package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SectorResponse(
        @JsonAlias({"id", "id_sector"}) Long idSector,
        @JsonAlias({"id_lugar"}) Long idLugar,
        @JsonAlias({"sector_nombre", "nombre"}) String nombre,
        @JsonAlias({"capacidad", "capacidad_max", "capacidadMaxima"}) Integer capacidadMax,
        String tipo,
        Integer filas,
        Integer asientosPorFila
) {

    /** Constructor previo, conservado para no modificar los tests existentes. */
    public SectorResponse(Long idSector, Long idLugar, String nombre, Integer capacidadMax) {
        this(idSector, idLugar, nombre, capacidadMax, null, null, null);
    }
}
