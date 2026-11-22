package com.ticketya.notificaciones.dto.response;

import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;

import java.time.Instant;

public record NotificacionResponse(
        Long id,
        String claveIdempotencia,
        Long idUsuario,
        TipoNotificacion tipo,
        String destinatario,
        String asunto,
        EstadoNotificacion estado,
        int intentos,
        Instant proximoIntentoEn,
        String ultimoError,
        String correlationId,
        Instant fechaCreacion,
        Instant fechaEnvio
) {
}
