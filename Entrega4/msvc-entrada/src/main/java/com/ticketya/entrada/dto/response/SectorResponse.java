package com.ticketya.entrada.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SectorResponse(
        @JsonAlias({"id", "id_sector"}) Long idSector,
        @JsonAlias({"capacidad", "capacidad_max", "capacidadMaxima"}) Integer capacidadMax,
        @JsonAlias({"filasSector", "filas_sector"}) Integer filas,
        @JsonAlias({"asientosPorFilaSector", "asientos_por_fila"}) Integer asientosPorFila,
        @JsonAlias("tipoSector") String tipo
) {
}
