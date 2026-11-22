package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.response.ReporteVentasEventoResponse;
import com.ticketya.entrada.service.ReporteVentasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno del reporte: lo consume msvc-evento por Feign. No tiene
 * ruta en el gateway (ADR-0001): el gateway deniega {@code /internal/**}.
 */
@RestController
@RequestMapping("/internal/entradas")
public class InternalReporteController {

    private final ReporteVentasService reporteVentasService;

    public InternalReporteController(ReporteVentasService reporteVentasService) {
        this.reporteVentasService = reporteVentasService;
    }

    @GetMapping("/eventos/{idEvento}/reporte")
    public ReporteVentasEventoResponse reporte(@PathVariable("idEvento") Long idEvento) {
        return reporteVentasService.reporte(idEvento);
    }
}
