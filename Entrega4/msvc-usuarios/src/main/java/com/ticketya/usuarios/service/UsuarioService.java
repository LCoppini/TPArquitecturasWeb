package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.ActualizarUsuarioRequest;
import com.ticketya.usuarios.dto.request.CrearUsuarioRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;

import java.util.List;
import java.util.Set;

public interface UsuarioService {
    UsuarioResponse crear(CrearUsuarioRequest request);

    List<UsuarioResponse> listar();

    UsuarioResponse buscarPorId(Long id);

    UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request);

    void eliminar(Long id);

    UsuarioResponse reemplazarRoles(Long id, Set<String> tiposRoles);
}
