package com.ticketya.ubicacion.controller;

import com.ticketya.ubicacion.dto.request.ActualizarLugarRequest;
import com.ticketya.ubicacion.dto.request.LugarRequest;
import com.ticketya.ubicacion.dto.request.SectorRequest;
import com.ticketya.ubicacion.dto.response.LugarResponse;
import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.exception.ErrorApiResponse;
import com.ticketya.ubicacion.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
@Tag(name = "Lugares", description = "Lugares (estadios, teatros, salas) y sus sectores")
public class LugarController {

    private final LugarService lugarService;

    public LugarController(LugarService lugarService) {
        this.lugarService = lugarService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un lugar con sus sectores")
    @ApiResponse(responseCode = "201", description = "Lugar creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos (coordenadas, sin sectores, capacidad)",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public LugarResponse crear(@Valid @RequestBody LugarRequest request) {
        return lugarService.crear(request);
    }

    @GetMapping
    @Operation(summary = "Listar lugares, opcionalmente por ciudad o por ids (ej. ids=1,2,3)")
    @ApiResponse(responseCode = "200", description = "Lugares con sus sectores")
    public List<LugarResponse> listar(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) List<Long> ids
    ) {
        return lugarService.listar(ciudad, ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un lugar con sus sectores")
    @ApiResponse(responseCode = "200", description = "Lugar encontrado")
    @ApiResponse(responseCode = "404", description = "El lugar no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public LugarResponse obtenerPorId(@PathVariable Long id) {
        return lugarService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar los datos de un lugar (los sectores no se modifican)")
    @ApiResponse(responseCode = "200", description = "Lugar actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "El lugar no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public LugarResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarLugarRequest request) {
        return lugarService.actualizar(id, request);
    }

    @PostMapping("/{id}/sectores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un sector a un lugar existente")
    @ApiResponse(responseCode = "201", description = "Sector creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "El lugar no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un sector con ese nombre en el lugar",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public SectorResponse agregarSector(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return lugarService.agregarSector(id, request);
    }

    @GetMapping("/{id}/sectores")
    @Operation(summary = "Listar los sectores de un lugar")
    @ApiResponse(responseCode = "200", description = "Sectores del lugar")
    @ApiResponse(responseCode = "404", description = "El lugar no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public List<SectorResponse> listarSectores(@PathVariable Long id) {
        return lugarService.listarSectores(id);
    }
}
