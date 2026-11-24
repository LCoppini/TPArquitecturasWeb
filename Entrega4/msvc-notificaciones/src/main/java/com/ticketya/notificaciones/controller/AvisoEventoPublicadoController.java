package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.request.EventoPublicadoRequest;
import com.ticketya.notificaciones.dto.response.EventoPublicadoResponse;
import com.ticketya.notificaciones.service.AvisoEventoPublicadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notificaciones")
@Tag(name = "Notificaciones", description = "Contrato interno entre microservicios (sin ruta en el gateway)")
public class AvisoEventoPublicadoController {

    private final AvisoEventoPublicadoService service;

    public AvisoEventoPublicadoController(AvisoEventoPublicadoService service) {
        this.service = service;
    }

    @PostMapping("/eventos-publicados")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Avisar a los clientes que siguen a un artista del evento en la ciudad del evento "
            + "(un email por usuario, idempotente por evento y usuario)")
    @ApiResponse(responseCode = "202", description = "Avisos registrados")
    public EventoPublicadoResponse eventoPublicado(@Valid @RequestBody EventoPublicadoRequest request) {
        return service.avisar(request);
    }
}
