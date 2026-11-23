package com.ticketya.notificaciones.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vista local de msvc-usuarios: solo lo que se necesita para escribir al cliente.
 * Hoy msvc-usuarios no expone email en su respuesta; hasta entonces el destinatario debe venir en la solicitud.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioResponse(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario,
        String nombre,
        String email
) {
}
