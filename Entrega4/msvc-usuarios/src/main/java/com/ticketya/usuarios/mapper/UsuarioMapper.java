package com.ticketya.usuarios.mapper;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Component
public class UsuarioMapper {

    public UsuarioResponse aResponse(Usuario usuario) {
        var roles = usuario.getRoles().stream()
                .sorted(Comparator.comparing(Rol::getTipo))
                .map(rol -> new RolResponse(rol.getIdRol(), rol.getTipo()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getDni(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCiudad(),
                usuario.isActivo(),
                roles
        );
    }
}
