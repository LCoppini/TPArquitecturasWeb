package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.SolicitudNotificacionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato interno de msvc-notificaciones, no expuesto a través del gateway. */
@FeignClient(name = "msvc-notificaciones", path = "/internal/notificaciones")
public interface NotificacionesFeign {

    @PostMapping
    void solicitar(@RequestBody SolicitudNotificacionRequest solicitud);
}
