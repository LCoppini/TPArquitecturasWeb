package com.ticketya.usuarios.dto.response;

/** Respuesta de login: solo el token y su metadata. Nunca incluye password ni hash. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos
) {
}
