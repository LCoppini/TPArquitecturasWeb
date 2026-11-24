package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.ClientesDeArtistaResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato interno: sin ruta en el gateway. Lo invoca msvc-notificaciones por Feign, sin JWT de usuario,
 * para saber a quien avisar cuando un artista publica un evento.
 */
@RestController
@RequestMapping("/internal/preferencias")
public class InternalArtistaController {

    private final PreferenciaService preferenciaService;

    public InternalArtistaController(PreferenciaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping("/artistas/{idArtista}/clientes")
    public ClientesDeArtistaResponse clientes(
            @PathVariable Long idArtista,
            @RequestParam String ciudad
    ) {
        return preferenciaService.clientesPorArtistaYCiudad(idArtista, ciudad);
    }
}
