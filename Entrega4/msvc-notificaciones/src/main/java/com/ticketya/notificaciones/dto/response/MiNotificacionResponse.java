package com.ticketya.notificaciones.dto.response;

import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;

import java.time.Instant;

/** Vista de una notificacion para su destinatario: sin errores internos ni claves de idempotencia. */
public record MiNotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        String asunto,
        EstadoNotificacion estado,
        Instant fechaCreacion,
        Instant fechaEnvio
) {

    public static MiNotificacionResponse desde(NotificacionResponse n) {
        return new MiNotificacionResponse(n.id(), n.tipo(), n.asunto(), n.estado(), n.fechaCreacion(), n.fechaEnvio());
    }
}
