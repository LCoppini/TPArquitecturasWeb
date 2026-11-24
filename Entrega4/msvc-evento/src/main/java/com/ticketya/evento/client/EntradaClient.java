package com.ticketya.evento.client;

import com.ticketya.evento.dto.request.ModificacionEventoRequest;
import com.ticketya.evento.dto.response.DisponibilidadResponse;
import com.ticketya.evento.dto.response.ReporteVentasEventoResponse;
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

    /** Reemplaza al path muerto /eventos/{idEvento}/ventas; también alimenta la relevancia. */
    @GetMapping("/internal/entradas/eventos/{idEvento}/reporte")
    ReporteVentasEventoResponse consultarReporte(@PathVariable("idEvento") Long idEvento);

    /** Contrato interno: entrada conoce a los compradores y les avisa del cambio. */
    @PostMapping("/internal/entradas/eventos/{idEvento}/modificacion")
    void notificarModificacion(
            @PathVariable("idEvento") Long idEvento,
            @RequestBody ModificacionEventoRequest request
    );

    @PostMapping("/api/admin/entradas/eventos/{idEvento}/cancelacion")
    void cancelarEntradas(
            @PathVariable("idEvento") Long idEvento,
            @RequestHeader("X-User-Id") Long idAdministrador
    );
}
