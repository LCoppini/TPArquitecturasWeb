package com.ticketya.evento.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public record ConfigurarSectoresRequest(
        @NotEmpty List<@Valid EventoSectorRequest> sectores
) {
    public ConfigurarSectoresRequest {
        sectores = sectores == null ? List.of() : new ArrayList<>(sectores);
    }
}
