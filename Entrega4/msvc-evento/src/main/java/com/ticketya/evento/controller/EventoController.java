package com.ticketya.evento.controller;

import com.ticketya.evento.dto.response.EventoPaginaResponse;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.EventoSectorResponse;
import com.ticketya.evento.service.EventoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Consultas públicas del catálogo. Las escrituras viven en los controllers por rol. */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/{id}")
    public EventoResponse obtenerPorId(@PathVariable Long id) {
        return eventoService.obtenerPorId(id);
    }

    @GetMapping("/{id}/sectores/{idSector}")
    public EventoSectorResponse obtenerPrecioSector(
            @PathVariable Long id,
            @PathVariable Long idSector
    ) {
        return eventoService.obtenerPrecioSector(id, idSector);
    }

    @GetMapping
    public EventoPaginaResponse buscar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long idOrganizador,
            @RequestParam(required = false) String participante,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "fecha") String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano
    ) {
        return eventoService.buscar(tipo, idOrganizador, participante, ciudad, desde, hasta,
                orden, pagina, tamano);
    }
}
