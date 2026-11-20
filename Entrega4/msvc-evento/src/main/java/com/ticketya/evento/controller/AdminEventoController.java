package com.ticketya.evento.controller;

import com.ticketya.evento.dto.request.CambioEstadoRequest;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Moderación administrativa de eventos (ADR-0001). */
@RestController
@RequestMapping("/api/admin/eventos")
public class AdminEventoController {

    private final EventoService eventoService;

    public AdminEventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping("/{id}/suspension")
    public EventoResponse suspender(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        return eventoService.suspender(id, idAdministrador);
    }

    @PutMapping("/{id}/estado")
    public EventoResponse cambiarEstado(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador,
            @Valid @RequestBody CambioEstadoRequest request
    ) {
        return eventoService.cambiarEstadoAdministrativo(id, idAdministrador, request.estado().trim());
    }

    @PostMapping("/{id}/cancelacion")
    public EventoResponse cancelar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        return eventoService.cancelar(id, idAdministrador);
    }

    @PostMapping("/{id}/cancelacion/reintentar")
    public EventoResponse reintentarCoordinacionCancelacion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        return eventoService.reintentarCoordinacionCancelacion(id, idAdministrador);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarFisicamente(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        eventoService.eliminarFisicamenteAdministrativo(id, idAdministrador);
    }
}
