package com.ticketya.evento.service.impl;

import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.exception.ReglaNegocioException;
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

    @Override
    @Transactional
    public ParticipanteResponse crear(ParticipanteRequest request) {
        String nombre = textoObligatorio(request.nombre(), "nombre");
        String tipo = textoObligatorio(request.tipo(), "tipo");
        return participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase(nombre, tipo)
                .map(eventoMapper::aResponse)
                .orElseGet(() -> eventoMapper.aResponse(
                        participanteRepository.save(new Participante(nombre, tipo))));
    }

    @Override
    @Transactional
    public ParticipanteResponse actualizar(Long id, ParticipanteRequest request) {
        var participante = participanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el participante con id " + id));
        String nombre = textoObligatorio(request.nombre(), "nombre");
        String tipo = textoObligatorio(request.tipo(), "tipo");
        participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase(nombre, tipo)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ReglaNegocioException("Ya existe un artista con ese nombre y tipo");
                });
        participante.setNombre(nombre);
        participante.setTipo(tipo);
        return eventoMapper.aResponse(participanteRepository.save(participante));
    }

    private String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El " + campo + " del artista es obligatorio");
        }
        return valor.trim();
    }
}
