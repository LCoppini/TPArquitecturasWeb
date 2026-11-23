package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.response.FilaEventoResponse;
import com.ticketya.entrada.dto.response.IngresoFilaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ticketya.entrada.service.FilaVirtualService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filas-virtuales/eventos/{idEvento}")
@Tag(name = "Fila virtual", description = "Ingreso y consulta del turno del cliente en la fila de un evento")
public class FilaVirtualController {

    private final FilaVirtualService filaVirtualService;

    public FilaVirtualController(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Operation(
            summary = "Ingresar a la fila",
            description = "X-User-Id se deriva del JWT en el gateway. Devuelve la membresía vigente o crea una nueva con secuencia inmutable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ingreso vigente devuelto"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "404", description = "El evento no tiene fila"),
            @ApiResponse(responseCode = "409", description = "Fila cerrada o sin cupo"),
            @ApiResponse(responseCode = "503", description = "Fila deshabilitada por ticketya.fila-virtual.enabled")
    })
    @PostMapping("/ingresos")
    public IngresoFilaResponse unirse(
            @PathVariable Long idEvento,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        return filaVirtualService.unirse(idEvento, idUsuario);
    }

    @Operation(
            summary = "Consultar mi estado en la fila",
            description = "Devuelve posición, secuencia, estado y vencimiento del turno vigente del cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del turno"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "404", description = "Sin membresía vigente para el evento")
    })
    @GetMapping("/mi-estado")
    public IngresoFilaResponse consultarMiEstado(
            @PathVariable Long idEvento,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        return filaVirtualService.consultarMiEstado(idEvento, idUsuario);
    }

}
