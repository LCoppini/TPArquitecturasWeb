package com.ticketya.evento.controller;

import com.ticketya.evento.dto.request.CambioEstadoRequest;
import com.ticketya.evento.dto.request.ConfigurarSectoresRequest;
import com.ticketya.evento.dto.request.EventoRequest;
import com.ticketya.evento.dto.response.EventoPaginaResponse;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.EventoSectorResponse;
import com.ticketya.evento.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse crear(
            @RequestHeader("X-User-Id") Long idSolicitante,
            @Valid @RequestBody EventoRequest request
    ) {
        return eventoService.crear(idSolicitante, request);
    }

    @GetMapping("/{id}")
    public EventoResponse obtenerPorId(@PathVariable Long id) {
        return eventoService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public EventoResponse actualizar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante,
            @Valid @RequestBody EventoRequest request
    ) {
        return eventoService.actualizar(id, idSolicitante, request);
    }

    @PutMapping("/{id}/sectores")
    public EventoResponse configurarSectores(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante,
            @Valid @RequestBody ConfigurarSectoresRequest request
    ) {
        return eventoService.configurarSectores(id, idSolicitante, request);
    }

    @GetMapping("/{id}/sectores/{idSector}")
    public EventoSectorResponse obtenerPrecioSector(
            @PathVariable Long id,
            @PathVariable Long idSector
    ) {
        return eventoService.obtenerPrecioSector(id, idSector);
    }

    @PostMapping("/{id}/publicacion")
    public EventoResponse publicar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante
    ) {
        return eventoService.publicar(id, idSolicitante);
    }

    @PostMapping("/{id}/suspension")
    public EventoResponse suspender(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante
    ) {
        return eventoService.suspender(id, idSolicitante);
    }

    @PutMapping("/{id}/estado")
    public EventoResponse cambiarEstado(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante,
            @Valid @RequestBody CambioEstadoRequest request
    ) {
        return eventoService.cambiarEstadoAdministrativo(id, idSolicitante, request.estado().trim());
    }

    @PostMapping("/{id}/cancelacion")
    public EventoResponse cancelar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante
    ) {
        return eventoService.cancelar(id, idSolicitante);
    }

    @PostMapping("/{id}/cancelacion/reintentar")
    public EventoResponse reintentarCoordinacionCancelacion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante
    ) {
        return eventoService.reintentarCoordinacionCancelacion(id, idSolicitante);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarFisicamente(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idSolicitante
    ) {
        eventoService.eliminarFisicamente(id, idSolicitante);
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
