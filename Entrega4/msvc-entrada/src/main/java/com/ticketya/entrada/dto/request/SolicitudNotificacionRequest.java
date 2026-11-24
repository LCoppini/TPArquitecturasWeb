package com.ticketya.entrada.dto.request;

import java.util.Map;

/** Contrato de POST /internal/notificaciones de msvc-notificaciones. */
public record SolicitudNotificacionRequest(
        String claveIdempotencia,
        Long idUsuario,
        String tipo,
        String destinatario,
        Map<String, String> datos
) {
}
