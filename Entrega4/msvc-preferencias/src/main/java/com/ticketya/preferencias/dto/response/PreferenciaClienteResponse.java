package com.ticketya.preferencias.dto.response;

import java.util.List;
import java.util.Map;

/**
 * DTO público: no expone el documento MongoDB ni sus conjuntos internos de
 * idempotencia/exclusión (comprasProcesadas, eventosComprados).
 */
public record PreferenciaClienteResponse(
        Long idUsuario,
        String ciudad,
        Map<String, Integer> tiposEvento,
        List<ArtistaPreferidoResponse> artistas
) {
}
