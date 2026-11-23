package com.ticketya.evento.client;

import com.ticketya.evento.dto.request.EventoPublicadoAvisoRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato interno: sin ruta en el gateway. contextId evita choques de beans con otros clients. */
@FeignClient(name = "msvc-notificaciones", contextId = "notificacionesClient", path = "/")
public interface NotificacionesClient {

    @PostMapping("/internal/notificaciones/eventos-publicados")
    void eventoPublicado(@RequestBody EventoPublicadoAvisoRequest request);
}
