package com.ticketya.evento.service;

import com.ticketya.evento.dto.response.ParticipanteResponse;

import java.util.List;

public interface ParticipanteService {

    ParticipanteResponse obtenerPorId(Long id);

    List<ParticipanteResponse> listar();
}
