package com.ticketya.entrada.dto.request;

import java.util.Map;

public record NotificacionRequest(
        String tipo,
        Long idUsuario,
        String correlationId,
        Map<String, Object> datos
) {
}
