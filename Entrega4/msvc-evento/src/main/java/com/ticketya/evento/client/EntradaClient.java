package com.ticketya.evento.client;

import com.ticketya.evento.dto.response.DisponibilidadResponse;
import com.ticketya.evento.dto.response.VentasEventoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "msvc-entrada", contextId = "entradaClient", path = "/")
public interface EntradaClient {

    /** Contrato interno: sin ruta en el gateway (ADR-0001). */
    @GetMapping("/internal/entradas/disponibilidad")
    DisponibilidadResponse consultarDisponibilidad(
            @RequestParam("idEvento") Long idEvento,
            @RequestParam("idSector") Long idSector
    );

    @GetMapping("/eventos/{idEvento}/ventas")
    VentasEventoResponse consultarVentas(@PathVariable("idEvento") Long idEvento);

    @PostMapping("/eventos/{idEvento}/cancelacion")
    void cancelarEntradas(
            @PathVariable("idEvento") Long idEvento,
            @RequestHeader("X-User-Id") Long idAdministrador
    );
}
