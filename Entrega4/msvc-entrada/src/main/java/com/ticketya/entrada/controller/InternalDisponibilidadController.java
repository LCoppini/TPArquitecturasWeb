package com.ticketya.entrada.controller;

import com.ticketya.entrada.service.EntradaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Contrato interno de disponibilidad: lo consume msvc-evento por Feign.
 * No tiene ruta en el gateway (ADR-0001): el gateway deniega `/internal/**`.
 */
@RestController
@RequestMapping("/internal/entradas")
public class InternalDisponibilidadController {

    private final EntradaService entradaService;

    public InternalDisponibilidadController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @GetMapping("/disponibilidad")
    public Map<String, Object> disponibilidad(
            @RequestParam Long idEvento,
            @RequestParam Long idSector
    ) {
        return entradaService.disponibilidad(idEvento, idSector);
    }
}
