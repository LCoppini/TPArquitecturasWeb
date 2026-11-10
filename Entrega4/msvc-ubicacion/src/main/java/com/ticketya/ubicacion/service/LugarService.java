package com.ticketya.ubicacion.service;

import com.ticketya.ubicacion.dto.request.ActualizarLugarRequest;
import com.ticketya.ubicacion.dto.request.LugarRequest;
import com.ticketya.ubicacion.dto.request.SectorRequest;
import com.ticketya.ubicacion.dto.response.LugarResponse;
import com.ticketya.ubicacion.dto.response.SectorResponse;

import java.util.List;

public interface LugarService {

    LugarResponse crear(LugarRequest request);

    LugarResponse obtenerPorId(Long id);

    List<LugarResponse> listar(String ciudad, List<Long> ids);

    LugarResponse actualizar(Long id, ActualizarLugarRequest request);

    SectorResponse agregarSector(Long idLugar, SectorRequest request);

    List<SectorResponse> listarSectores(Long idLugar);
}
