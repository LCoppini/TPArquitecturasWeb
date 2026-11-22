package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.RecomendacionResponse;
import com.ticketya.preferencias.service.RecomendacionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones/clientes")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @GetMapping("/{idUsuario}")
    public List<RecomendacionResponse> recomendar(@PathVariable Long idUsuario,
                                                  @RequestHeader("X-User-Id") Long idSolicitante) {
        if (!idUsuario.equals(idSolicitante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El cliente solo puede consultar sus propias recomendaciones");
        }
        return recomendacionService.recomendar(idUsuario);
    }
}
