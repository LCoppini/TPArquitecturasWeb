package com.ticketya.usuarios.dto.response;

import java.util.Set;

public record UsuarioResponse(
        Long idUsuario,
        Long dni,
        String nombre,
        String apellido,
        String ciudad,
        boolean activo,
        Set<RolResponse> roles
) {
}
