package com.ticketya.evento.controller;

import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.service.ParticipanteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Administración de artistas: el gateway autoriza el prefijo a ADMINISTRADOR (ADR-0001). */
@RestController
@RequestMapping("/api/admin/artistas")
public class AdminParticipanteController {

    private final ParticipanteService participanteService;

    public AdminParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @PostMapping
    public ParticipanteResponse crear(@Valid @RequestBody ParticipanteRequest request) {
        return participanteService.crear(request);
    }

    @PutMapping("/{id}")
    public ParticipanteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ParticipanteRequest request
    ) {
        return participanteService.actualizar(id, request);
    }
}
