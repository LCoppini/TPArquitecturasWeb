package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.CrearUsuarioAdministrativoRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;

/** Alta de usuarios con roles privilegiados, reservada a ADMINISTRADOR. */
public interface AdminUsuarioService {

    UsuarioResponse crear(CrearUsuarioAdministrativoRequest request);
}
