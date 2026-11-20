package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.LoginRequest;
import com.ticketya.usuarios.dto.response.LoginResponse;

/** Valida credenciales y devuelve un JWT RS256. */
public interface AutenticacionService {

    LoginResponse login(LoginRequest request);
}
