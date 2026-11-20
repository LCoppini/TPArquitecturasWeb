package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.service.EntradaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @PostMapping
    public EntradaResponse crear(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody EntradaRequest entrada
    ) {
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
    public List<EntradaResponse> listar(@RequestHeader("X-User-Id") Long idUsuario) {
        return entradaService.listarDeUsuario(idUsuario);
    }

    @GetMapping("/{id}")
    public EntradaResponse buscarPorId(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        return entradaService.buscarPorIdDeUsuario(id, idUsuario);
    }

    @PostMapping("/{id}/devolucion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarDevolucion(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        entradaService.solicitarDevolucion(id, idUsuario);
    }
}
