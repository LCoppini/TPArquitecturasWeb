package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.request.CompraDevolucionRequest;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.CancelacionEventoService;
import com.ticketya.entrada.service.EntradaService;
import com.ticketya.entrada.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones administrativas sobre entradas (ADR-0001). */
@RestController
@RequestMapping("/api/admin/entradas")
public class AdminEntradaController {

    private final EntradaService entradaService;
    private final PagoService pagoService;
    private final CancelacionEventoService cancelacionEventoService;
    private final CompraService compraService;

    public AdminEntradaController(
            EntradaService entradaService,
            PagoService pagoService,
            CancelacionEventoService cancelacionEventoService,
            CompraService compraService
    ) {
        this.entradaService = entradaService;
        this.pagoService = pagoService;
        this.cancelacionEventoService = cancelacionEventoService;
        this.compraService = compraService;
    }

    @PutMapping("/{id}/devolucion")
    public ResponseEntity<Void> resolverDevolucion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador,
            @RequestBody EntradaRequest request
    ) {
        boolean aprobada = entradaService.resolverDevolucion(
                id,
                Boolean.TRUE.equals(request.getAprobarDevolucion())
        );
        if (aprobada) {
            pagoService.reintegrarEntrada(id);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devolucion/reintentar")
    public ResponseEntity<Void> reintentarDevolucion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        pagoService.reintegrarEntrada(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/eventos/{idEvento}/cancelacion")
    public ResponseEntity<Void> cancelarEvento(
            @PathVariable Long idEvento,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        cancelacionEventoService.cancelarEvento(idEvento);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/compras/{idCompra}/devolucion")
    public ResponseEntity<Void> resolverDevolucionCompra(
            @PathVariable Long idCompra,
            @RequestHeader("X-User-Id") Long idAdministrador,
            @RequestBody @jakarta.validation.Valid CompraDevolucionRequest request
    ) {
        compraService.resolverDevolucion(idCompra, Boolean.TRUE.equals(request.aprobarDevolucion()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compras/{idCompra}/devolucion/reintentar")
    public ResponseEntity<Void> reintentarDevolucionCompra(
            @PathVariable Long idCompra,
            @RequestHeader("X-User-Id") Long idAdministrador
    ) {
        compraService.reintentarDevolucion(idCompra);
        return ResponseEntity.accepted().build();
    }
}
