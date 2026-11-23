package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.NotificacionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Notificación interna, no expuesta a través del gateway. */
@FeignClient(name = "msvc-notificaciones", path = "/api/notificaciones")
public interface NotificacionesClient {

    @PostMapping
    void enviar(@RequestBody NotificacionRequest notificacion);
}
