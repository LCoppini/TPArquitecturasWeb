package com.ticketya.usuarios.service;

import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioCiudadServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioServiceImpl service =
            new UsuarioServiceImpl(repository, mock(RolRepository.class), new UsuarioMapper());

    @Test
    void cambiaSoloLaCiudadYLaRecorta() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(9L);
        usuario.setDni(30111222L);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCiudad("Tandil");
        usuario.setActivo(true);
        when(repository.findByIdUsuarioAndActivoTrue(9L)).thenReturn(Optional.of(usuario));
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.actualizarCiudad(9L, " Rosario ");

        assertEquals("Rosario", respuesta.ciudad());
        assertEquals(30111222L, respuesta.dni());
        assertEquals("Ana", respuesta.nombre());
    }

    @Test
    void usuarioInactivoOInexistenteLanzaNoEncontrado() {
        when(repository.findByIdUsuarioAndActivoTrue(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarCiudad(9L, "Rosario"));
        verify(repository, never()).saveAndFlush(any());
    }
}
