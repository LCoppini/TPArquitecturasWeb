package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.RegistroPagoRequest;
import com.ticketya.entrada.dto.response.RegistroPagoResponse;
import com.ticketya.entrada.service.RegistroPagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/registros-pago")
public class RegistroPagoController {

    private final RegistroPagoService registroPagoService;

    public RegistroPagoController(RegistroPagoService registroPagoService) {
        this.registroPagoService = registroPagoService;
    }

    @PostMapping
    public RegistroPagoResponse crear(@Valid @RequestBody RegistroPagoRequest registroPago) {
        return registroPagoService.crear(registroPago);
    }

    @GetMapping
    public List<RegistroPagoResponse> listar(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRolCliente(rol);
        return registroPagoService.listar(idUsuario);
    }

    @GetMapping("/{idEntrada}/{idPago}")
    public RegistroPagoResponse buscarPorId(
            @PathVariable Long idEntrada,
            @PathVariable Long idPago,
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRolCliente(rol);
        return registroPagoService.buscarPorId(idEntrada, idPago, idUsuario);
    }

    @PutMapping("/{idEntrada}/{idPago}")
    public RegistroPagoResponse actualizar(
            @PathVariable Long idEntrada,
            @PathVariable Long idPago,
            @Valid @RequestBody RegistroPagoRequest registroPago
    ) {
        return registroPagoService.actualizar(idEntrada, idPago, registroPago);
    }

    @DeleteMapping("/{idEntrada}/{idPago}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idEntrada, @PathVariable Long idPago) {
        registroPagoService.eliminar(idEntrada, idPago);
        return ResponseEntity.noContent().build();
    }

    private void exigirRolCliente(String recibido) {
        String rol = recibido.startsWith("ROLE_") ? recibido.substring(5) : recibido;
        if (!"CLIENTE".equalsIgnoreCase(rol)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Se requiere el rol CLIENTE"
            );
        }
    }
}
