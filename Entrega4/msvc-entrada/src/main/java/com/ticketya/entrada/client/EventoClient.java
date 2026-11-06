package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.response.EventoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Obtiene el estado y la fecha desde el servicio dueño del evento.
 */
@FeignClient(name = "msvc-evento", path = "/api/eventos")
public interface EventoClient {

    @GetMapping("/{id}")
    EventoResponse obtenerPorId(@PathVariable("id") Long id);
}
