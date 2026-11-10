package com.ticketya.evento.controller;

import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.service.ParticipanteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<ParticipanteResponse> listar() {
        return participanteService.listar();
    }

    @GetMapping("/{id}")
    public ParticipanteResponse obtenerPorId(@PathVariable Long id) {
        return participanteService.obtenerPorId(id);
    }
}
