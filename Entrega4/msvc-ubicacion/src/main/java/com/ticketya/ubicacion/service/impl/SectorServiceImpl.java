package com.ticketya.ubicacion.service.impl;

import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.exception.RecursoNoEncontradoException;
import com.ticketya.ubicacion.mapper.UbicacionMapper;
import com.ticketya.ubicacion.repository.SectorRepository;
import com.ticketya.ubicacion.service.SectorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SectorServiceImpl implements SectorService {

    private final SectorRepository sectorRepository;
    private final UbicacionMapper mapper;

    public SectorServiceImpl(SectorRepository sectorRepository, UbicacionMapper mapper) {
        this.sectorRepository = sectorRepository;
        this.mapper = mapper;
    }

    @Override
    public SectorResponse obtenerPorId(Long id) {
        return sectorRepository.findById(id)
                .map(mapper::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el sector con id " + id));
    }
}
