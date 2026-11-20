package com.ticketya.usuarios.config;

import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministradorSeederTest {

    @Test
    void sinVariablesNoCreaAdministrador() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        new AdministradorSeeder(usuarios, roles, encoder, "", "", "", "", "", "").run(null);

        verify(usuarios, never()).saveAndFlush(any());
        verify(roles, never()).insertarSiNoExiste(any());
    }

    @Test
    void conVariablesCreaAdministradorConPasswordHasheado() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Rol administrador = new Rol("ADMINISTRADOR");
        administrador.setIdRol(4L);
        when(usuarios.existsByEmail("admin@ticketya.local")).thenReturn(false);
        when(roles.findAllByTipoIn(Set.of("ADMINISTRADOR"))).thenReturn(List.of(administrador));
        when(encoder.encode("secreto-admin")).thenReturn("hash-admin");

        new AdministradorSeeder(usuarios, roles, encoder,
                " ADMIN@Ticketya.local ", "secreto-admin", "0", "Admin", "TicketYa", "Tandil").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        Usuario creado = captor.getValue();
        assertEquals("admin@ticketya.local", creado.getEmail());
        assertEquals("hash-admin", creado.getPasswordHash());
        assertEquals(Set.of(administrador), creado.getRoles());
        verify(roles).insertarSiNoExiste("ADMINISTRADOR");
    }

    @Test
    void noDuplicaSiYaExisteElEmail() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.existsByEmail("admin@ticketya.local")).thenReturn(true);

        new AdministradorSeeder(usuarios, roles, encoder,
                "admin@ticketya.local", "secreto-admin", "0", "Admin", "TicketYa", "Tandil").run(null);

        verify(usuarios, never()).saveAndFlush(any());
        verify(encoder, never()).encode(any());
    }
}
