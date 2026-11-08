package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.service.RolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public List<RolResponse> listar() {
        return rolRepository.findAllByOrderByTipoAsc().stream()
                .map(rol -> new RolResponse(rol.getIdRol(), rol.getTipo()))
                .toList();
    }
}
