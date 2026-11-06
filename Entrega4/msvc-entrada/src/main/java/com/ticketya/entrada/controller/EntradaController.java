package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.service.EntradaService;
import com.ticketya.entrada.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EntradaService entradaService;
    private final PagoService pagoService;

    public EntradaController(EntradaService entradaService, PagoService pagoService) {
        this.entradaService = entradaService;
        this.pagoService = pagoService;
    }

    @PostMapping
    public EntradaResponse crear(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody EntradaRequest entrada
    ) {
        exigirRol(rol, "CLIENTE");
        entrada.setIdUsuario(idUsuario);
        entrada.setClaveIdempotencia(clave);
        return entradaService.crear(entrada);
    }

    @GetMapping("/disponibilidad")
    public java.util.Map<String, Object> disponibilidad(
            @RequestParam Long idEvento,
            @RequestParam Long idSector
    ) {
        return entradaService.disponibilidad(idEvento, idSector);
    }

    @GetMapping
    public List<EntradaResponse> listar(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRol(rol, "CLIENTE");
        return entradaService.listarDeUsuario(idUsuario);
    }

    @GetMapping("/{id}")
    public EntradaResponse buscarPorId(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRol(rol, "CLIENTE");
        return entradaService.buscarPorIdDeUsuario(id, idUsuario);
    }

    @PutMapping("/{id}")
    public EntradaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EntradaRequest entrada
    ) {
        return entradaService.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        entradaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devolucion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarDevolucion(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long idUsuario,
        @RequestHeader("X-User-Role") String rol
    ) {
        exigirRol(rol, "CLIENTE");
    entradaService.solicitarDevolucion(id, idUsuario);
    }

    @PutMapping("/{id}/devolucion")
    public ResponseEntity<Void> resolverDevolucion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol,
            @RequestBody EntradaRequest request
    ) {
        exigirRol(rol, "ADMINISTRADOR");
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
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRol(rol, "ADMINISTRADOR");
        pagoService.reintegrarEntrada(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/eventos/{idEvento}/cancelacion")
    public ResponseEntity<Void> cancelarEvento(
            @PathVariable Long idEvento,
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol
    ) {
        exigirRol(rol, "ADMINISTRADOR");
        entradaService.cancelarEvento(idEvento);
        pagoService.cancelarEvento(idEvento);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/accesos/validar")
    public EntradaResponse validarQr(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("X-User-Role") String rol,
            @Valid @RequestBody EntradaRequest request
    ) {
        exigirRol(rol, "PERSONAL_ACCESO");
        return entradaService.validarQr(request.getCodigoQr(), request.getIdEvento());
    }

    private void exigirRol(String recibido, String requerido) {
        String rol = recibido.startsWith("ROLE_") ? recibido.substring(5) : recibido;
        if (!requerido.equalsIgnoreCase(rol)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Se requiere el rol " + requerido
            );
        }
    }
}
