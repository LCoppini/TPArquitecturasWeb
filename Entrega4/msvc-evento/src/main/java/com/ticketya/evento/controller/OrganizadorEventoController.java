package com.ticketya.evento.controller;

import com.ticketya.evento.dto.request.ConfigurarSectoresRequest;
import com.ticketya.evento.dto.request.EventoRequest;
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

/** Escrituras de un organizador sobre sus propios eventos (ADR-0001). */
@RestController
@RequestMapping("/api/organizador/eventos")
public class OrganizadorEventoController {

    private final EventoService eventoService;

    public OrganizadorEventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse crear(
            @RequestHeader("X-User-Id") Long idOrganizador,
            @Valid @RequestBody EventoRequest request
    ) {
        return eventoService.crear(idOrganizador, request);
    }

    @PutMapping("/{id}")
    public EventoResponse actualizar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idOrganizador,
            @Valid @RequestBody EventoRequest request
    ) {
        return eventoService.actualizar(id, idOrganizador, request);
    }

    @PutMapping("/{id}/sectores")
    public EventoResponse configurarSectores(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idOrganizador,
            @Valid @RequestBody ConfigurarSectoresRequest request
    ) {
        return eventoService.configurarSectores(id, idOrganizador, request);
    }

    @PostMapping("/{id}/publicacion")
    public EventoResponse publicar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idOrganizador
    ) {
        return eventoService.publicar(id, idOrganizador);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarFisicamente(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idOrganizador
    ) {
        eventoService.eliminarFisicamente(id, idOrganizador);
    }
}
