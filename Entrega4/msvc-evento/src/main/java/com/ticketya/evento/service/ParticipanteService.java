package com.ticketya.evento.service;

import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.ParticipanteResponse;

import java.util.List;

public interface ParticipanteService {

    ParticipanteResponse obtenerPorId(Long id);

    List<ParticipanteResponse> listar();

    ParticipanteResponse crear(ParticipanteRequest request);

    ParticipanteResponse actualizar(Long id, ParticipanteRequest request);
}
