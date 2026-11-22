package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.RecomendacionResponse;
import com.ticketya.preferencias.service.RecomendacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recomendaciones/clientes")
public class RecomendacionAdminController {

    private final RecomendacionService recomendacionService;

    public RecomendacionAdminController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @GetMapping("/{idUsuario}")
    public List<RecomendacionResponse> recomendar(@PathVariable Long idUsuario) {
        return recomendacionService.recomendar(idUsuario);
    }
}
