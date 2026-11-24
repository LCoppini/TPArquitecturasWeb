package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.EventoModificadoRequest;
import com.ticketya.entrada.service.ModificacionEventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno: lo invoca msvc-evento por Feign despues de modificar un evento.
 * No tiene ruta en el gateway (ADR-0001): el gateway deniega `/internal/**`.
 */
@RestController
@RequestMapping("/internal/entradas")
public class InternalEventoModificadoController {

    private final ModificacionEventoService modificacionEventoService;

    public InternalEventoModificadoController(ModificacionEventoService modificacionEventoService) {
        this.modificacionEventoService = modificacionEventoService;
    }

    @PostMapping("/eventos/{idEvento}/modificacion")
    public ResponseEntity<Void> modificado(
            @PathVariable Long idEvento,
            @Valid @RequestBody EventoModificadoRequest request
    ) {
        modificacionEventoService.notificarModificacion(idEvento, request.cambios());
        return ResponseEntity.accepted().build();
    }
}
