package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.dto.response.CompraRegistradaResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno: no tiene ruta en el gateway. Lo invoca el worker de msvc-entrada
 * por Feign, sin JWT de usuario.
 */
@RestController
@RequestMapping("/internal/preferencias")
public class InternalCompraController {

    private final PreferenciaService preferenciaService;

    public InternalCompraController(PreferenciaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @PostMapping("/compras")
    public ResponseEntity<CompraRegistradaResponse> registrar(@Valid @RequestBody CompraRegistradaRequest compra) {
        CompraRegistradaResponse respuesta = preferenciaService.registrarCompra(compra);
        HttpStatus estado = respuesta.yaProcesada() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(respuesta);
    }
}
