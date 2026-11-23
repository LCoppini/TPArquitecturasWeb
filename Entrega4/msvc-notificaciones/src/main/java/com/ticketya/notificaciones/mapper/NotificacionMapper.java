package com.ticketya.notificaciones.mapper;

import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.Notificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {

    public NotificacionResponse aResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getClaveIdempotencia(),
                notificacion.getIdUsuario(),
                notificacion.getTipo(),
                notificacion.getDestinatario(),
                notificacion.getAsunto(),
                notificacion.getEstado(),
                notificacion.getIntentos(),
                notificacion.getProximoIntentoEn(),
                notificacion.getUltimoError(),
                notificacion.getCorrelationId(),
                notificacion.getFechaCreacion(),
                notificacion.getFechaEnvio()
        );
    }
}
