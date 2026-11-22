package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.PreferenciaClienteResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Path de ADMINISTRADOR. El gateway autoriza el rol; el servicio no recibe ni consulta
 * el rol y no compara el id del path contra X-User-Id.
 */
@RestController
@RequestMapping("/api/admin/preferencias/clientes")
public class PreferenciaAdminController {

    private final PreferenciaService preferenciaService;

    public PreferenciaAdminController(PreferenciaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping("/{idUsuario}")
    public PreferenciaClienteResponse obtener(@PathVariable Long idUsuario) {
        return preferenciaService.obtener(idUsuario);
    }
}
