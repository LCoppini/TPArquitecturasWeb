package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.QrEscaneoRequest;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.service.EntradaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Control de acceso con QR para PERSONAL_ACCESO (ADR-0001). */
@RestController
@RequestMapping("/api/accesos")
public class AccesoController {

    private final EntradaService entradaService;

    public AccesoController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @PostMapping("/escanear")
    public EntradaResponse escanear(
            @RequestHeader("X-User-Id") Long idPersonal,
            @Valid @RequestBody QrEscaneoRequest request
    ) {
        return entradaService.validarQr(request.getCodigoQr(), request.getIdEvento(), idPersonal);
    }
}
