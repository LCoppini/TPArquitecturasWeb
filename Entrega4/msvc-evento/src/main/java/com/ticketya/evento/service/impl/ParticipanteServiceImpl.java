package com.ticketya.evento.service.impl;

import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.ParticipanteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParticipanteServiceImpl implements ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final EventoMapper eventoMapper;

    public ParticipanteServiceImpl(ParticipanteRepository participanteRepository, EventoMapper eventoMapper) {
        this.participanteRepository = participanteRepository;
        this.eventoMapper = eventoMapper;
    }

    @Override
    public ParticipanteResponse obtenerPorId(Long id) {
        return participanteRepository.findById(id)
                .map(eventoMapper::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el participante con id " + id));
    }

    @Override
    public List<ParticipanteResponse> listar() {
        return participanteRepository.findAll().stream()
                .map(eventoMapper::aResponse)
                .toList();
    }
}
