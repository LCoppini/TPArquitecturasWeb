package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.PreferenciaClienteResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Path de CLIENTE. El gateway ya autorizó el rol por path y solo inyecta X-User-Id;
 * aquí se comprueba únicamente la pertenencia del recurso al usuario autenticado.
 */
@RestController
@RequestMapping("/api/preferencias/clientes")
public class PreferenciaController {

    private final PreferenciaService preferenciaService;

    public PreferenciaController(PreferenciaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping("/{idUsuario}")
    public PreferenciaClienteResponse obtener(@PathVariable Long idUsuario,
                                              @RequestHeader("X-User-Id") Long idSolicitante) {
        if (!idUsuario.equals(idSolicitante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El cliente solo puede consultar su propia preferencia");
        }
        return preferenciaService.obtener(idUsuario);
    }
}
