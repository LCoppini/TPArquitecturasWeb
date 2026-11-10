package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Obtiene los datos del evento y el precio oficial de cada sector desde su servicio dueño.
 */
@FeignClient(name = "msvc-evento", path = "/api/eventos")
public interface EventoClient {

    @GetMapping("/{id}")
    EventoResponse obtenerPorId(@PathVariable("id") Long id);

    @GetMapping("/{id}/sectores/{idSector}")
    EventoSectorResponse obtenerPrecioSector(
            @PathVariable("id") Long idEvento,
            @PathVariable("idSector") Long idSector
    );
}
