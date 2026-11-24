package com.ticketya.usuarios.mapper;

import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioMapperEmailTest {

    @Test
    void laRespuestaIncluyeElEmailDelUsuario() {
        Usuario usuario = new Usuario();
        usuario.setDni(30111222L);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCiudad("Tandil");
        usuario.setEmail("ana@ticketya.dev");

        UsuarioResponse respuesta = new UsuarioMapper().aResponse(usuario);

        assertEquals("ana@ticketya.dev", respuesta.email());
    }
}
