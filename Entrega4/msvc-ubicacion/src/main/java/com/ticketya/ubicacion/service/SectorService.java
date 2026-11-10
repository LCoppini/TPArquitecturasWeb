package com.ticketya.ubicacion.service;

import com.ticketya.ubicacion.dto.response.SectorResponse;

public interface SectorService {

    SectorResponse obtenerPorId(Long id);
}
