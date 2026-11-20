package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.RegistroRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;

/** Alta publica: siempre crea usuarios CLIENTE. */
public interface RegistroService {

    UsuarioResponse registrar(RegistroRequest request);
}
