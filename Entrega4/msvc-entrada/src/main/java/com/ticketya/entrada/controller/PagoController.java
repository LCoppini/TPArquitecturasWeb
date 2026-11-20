package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.dto.response.PagoResponse;
import com.ticketya.entrada.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public PagoResponse crear(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody PagoRequest pago
    ) {
        pago.setIdUsuario(idUsuario);
        pago.setClaveIdempotencia(clave);
        return pagoService.crear(pago);
    }

    @GetMapping
    public List<PagoResponse> listar(@RequestHeader("X-User-Id") Long idUsuario) {
        return pagoService.listarDeUsuario(idUsuario);
    }

    @GetMapping("/{id}")
    public PagoResponse buscarPorId(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        return pagoService.buscarPorIdDeUsuario(id, idUsuario);
    }

    @PutMapping("/{id}")
    public PagoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PagoRequest pago
    ) {
        return pagoService.actualizar(id, pago);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
