package com.ticketya.ubicacion.controller;

import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.exception.ErrorApiResponse;
import com.ticketya.ubicacion.service.SectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectores")
@Tag(name = "Sectores", description = "Consulta de sectores por id (usada por Evento y Entrada)")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un sector por id (estructura y capacidad, sin precio)")
    @ApiResponse(responseCode = "200", description = "Sector encontrado")
    @ApiResponse(responseCode = "404", description = "El sector no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public SectorResponse obtenerPorId(@PathVariable Long id) {
        return sectorService.obtenerPorId(id);
    }
}
