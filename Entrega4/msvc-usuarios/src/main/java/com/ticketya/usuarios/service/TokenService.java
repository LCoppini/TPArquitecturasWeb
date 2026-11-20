package com.ticketya.usuarios.service;

import java.util.Set;

/** Emite el JWT RS256 según el contrato de ADR-0003. */
public interface TokenService {

    String emitir(Long idUsuario, Set<String> roles);

    long expiracionEnSegundos();
}
