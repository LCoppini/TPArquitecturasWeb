package com.ticketya.evento.client;

import com.ticketya.evento.dto.response.DisponibilidadResponse;
import com.ticketya.evento.dto.response.VentasEventoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "msvc-entrada", path = "/api/entradas")
public interface EntradaClient {

    @GetMapping("/disponibilidad")
    DisponibilidadResponse consultarDisponibilidad(
            @RequestParam("idEvento") Long idEvento,
            @RequestParam("idSector") Long idSector
    );

    @GetMapping("/eventos/{idEvento}/ventas")
    VentasEventoResponse consultarVentas(@PathVariable("idEvento") Long idEvento);

    @PostMapping("/eventos/{idEvento}/cancelacion")
    void cancelarEntradas(
            @PathVariable("idEvento") Long idEvento,
            @RequestHeader("X-User-Id") Long idAdministrador,
            @RequestHeader("X-User-Role") String rol
    );
}
