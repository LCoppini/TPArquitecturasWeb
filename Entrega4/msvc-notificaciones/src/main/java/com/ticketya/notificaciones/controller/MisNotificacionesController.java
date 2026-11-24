package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.response.MiNotificacionResponse;
import com.ticketya.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Historial del usuario autenticado; el gateway deriva su identidad del token y la envia en X-User-Id. */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Mis notificaciones", description = "Avisos enviados al usuario autenticado")
public class MisNotificacionesController {

    private final NotificacionService notificacionService;

    public MisNotificacionesController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/mias")
    @Operation(summary = "Mis notificaciones (hasta 100, la mas reciente primero)")
    @ApiResponse(responseCode = "200", description = "Notificaciones del usuario autenticado")
    public List<MiNotificacionResponse> mias(@RequestHeader("X-User-Id") Long idUsuario) {
        return notificacionService.listar(idUsuario, null).stream().map(MiNotificacionResponse::desde).toList();
    }
}
