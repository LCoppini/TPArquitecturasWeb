package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.response.FilaEventoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ticketya.entrada.service.FilaVirtualService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizador/filas-virtuales/eventos/{idEvento}")
@Tag(name = "Administración de fila virtual", description = "Activación y operaciones del organizador sobre la fila de un evento")
public class AdminFilaVirtualController {

    private final FilaVirtualService filaVirtualService;

    public AdminFilaVirtualController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Operation(
            summary = "Activar o reconfigurar la fila",
            description = "Sin cuerpo usa los límites por defecto; con ActivarFilaRequest fija maxPersonas y cupoEntradas. Una fila ACTIVA solo admite la misma configuración."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila activada o reconfigurada"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id o parámetros inválidos"),
            @ApiResponse(responseCode = "403", description = "El usuario no es organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición no permitida o parámetros distintos de los vigentes"),
            @ApiResponse(responseCode = "503", description = "Fila deshabilitada por configuración")
    })
    @PutMapping("/activacion")
    public FilaEventoResponse activar(
            @PathVariable Long idEvento,
            @RequestHeader("X-User-Id") Long idOrganizador,
            @RequestBody(required = false) ActivarFilaRequest request
    ) {
        return filaVirtualService.activar(idEvento, idOrganizador, request);
    }

    @Operation(
            summary = "Administrar la fila",
            description = "Operaciones: PAUSAR, REANUDAR y REABRIR. Cualquier transición no listada responde 409."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación aplicada"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "403", description = "El usuario no es organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Fila no encontrada"),
            @ApiResponse(responseCode = "409", description = "Operación de fila no permitida")
    })
    @PostMapping("/{operacion}")
    public FilaEventoResponse administrar(
            @PathVariable Long idEvento,
            @PathVariable String operacion,
            @RequestHeader("X-User-Id") Long idOrganizador
    ) {
        return filaVirtualService.administrar(idEvento, idOrganizador, operacion);
    }
}
