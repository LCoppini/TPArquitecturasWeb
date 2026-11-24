package com.ticketya.usuarios.dto.response;

import java.util.Set;

public record UsuarioResponse(
        Long idUsuario,
        Long dni,
        String nombre,
        String apellido,
        String ciudad,
        boolean activo,
        Set<RolResponse> roles,
        String email
) {

    /** Constructor previo, conservado para no modificar los tests existentes. */
    public UsuarioResponse(
            Long idUsuario,
            Long dni,
            String nombre,
            String apellido,
            String ciudad,
            boolean activo,
            Set<RolResponse> roles
    ) {
        this(idUsuario, dni, nombre, apellido, ciudad, activo, roles, null);
    }
}
