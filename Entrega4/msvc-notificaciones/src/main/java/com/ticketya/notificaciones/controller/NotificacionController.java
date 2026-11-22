package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.exception.ErrorApiResponse;
import com.ticketya.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/notificaciones")
@Tag(name = "Notificaciones", description = "Contrato interno entre microservicios (sin ruta en el gateway): "
        + "solicitud de envio de emails e historial de envios")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar el envio de una notificacion por email (asincrono e idempotente por clave)")
    @ApiResponse(responseCode = "202", description = "Notificacion registrada (o la ya existente con esa clave); "
            + "el email se envia despues")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o faltan datos obligatorios del tipo",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public NotificacionResponse solicitar(@Valid @RequestBody SolicitudNotificacionRequest request) {
        return notificacionService.solicitar(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el estado de una notificacion")
    @ApiResponse(responseCode = "200", description = "Notificacion encontrada")
    @ApiResponse(responseCode = "404", description = "La notificacion no existe",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public NotificacionResponse obtenerPorId(@PathVariable Long id) {
        return notificacionService.obtenerPorId(id);
    }

    @GetMapping
    @Operation(summary = "Historial de envios (hasta 100, la mas reciente primero), opcionalmente por usuario y estado")
    @ApiResponse(responseCode = "200", description = "Notificaciones encontradas")
    @ApiResponse(responseCode = "400", description = "Estado desconocido",
            content = @Content(schema = @Schema(implementation = ErrorApiResponse.class)))
    public List<NotificacionResponse> listar(
            @RequestParam(required = false) Long idUsuario,
            @RequestParam(required = false) EstadoNotificacion estado
    ) {
        return notificacionService.listar(idUsuario, estado);
    }
}
