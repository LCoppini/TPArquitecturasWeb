package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;

import java.util.List;

public interface NotificacionService {

    /** Registra la notificacion como PENDIENTE (idempotente por clave) y dispara el envio asincrono. */
    NotificacionResponse solicitar(SolicitudNotificacionRequest request);

    NotificacionResponse obtenerPorId(Long id);

    /** Hasta 100, la mas reciente primero; filtros opcionales. */
    List<NotificacionResponse> listar(Long idUsuario, EstadoNotificacion estado);
}
