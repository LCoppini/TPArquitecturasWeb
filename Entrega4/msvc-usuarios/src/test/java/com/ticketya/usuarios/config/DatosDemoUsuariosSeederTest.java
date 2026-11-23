package com.ticketya.usuarios.config;

import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosDemoUsuariosSeederTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DatosDemoUsuariosSeeder seeder() {
        return new DatosDemoUsuariosSeeder(usuarioRepository, rolRepository, passwordEncoder, "clave-demo-123");
    }

    @Test
    void creaUnUsuarioPorCadaCorreoQueNoExiste() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findAllByTipoIn(any())).thenReturn(List.of(new Rol("CLIENTE")));
        when(passwordEncoder.encode("clave-demo-123")).thenReturn("hash");

        seeder().run(null);

        ArgumentCaptor<Usuario> guardados = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(5)).saveAndFlush(guardados.capture());
        assertTrue(guardados.getAllValues().stream().allMatch(u -> "hash".equals(u.getPasswordHash())));
        assertEquals("admin@ticketya.dev", guardados.getAllValues().get(0).getEmail());
    }

    @Test
    void esIdempotenteSiLosUsuariosYaExisten() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        seeder().run(null);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(rolRepository, never()).insertarSiNoExiste(anyString());
    }

    @Test
    void asignaUnRolPorUsuarioSegunLaDefinicion() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findAllByTipoIn(any())).thenAnswer(invocacion -> {
            Set<String> tipos = invocacion.getArgument(0);
            return List.of(new Rol(tipos.iterator().next()));
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        seeder().run(null);

        verify(rolRepository).insertarSiNoExiste("ADMINISTRADOR");
        verify(rolRepository).insertarSiNoExiste("ORGANIZADOR");
        verify(rolRepository).insertarSiNoExiste("PERSONAL_ACCESO");
        verify(rolRepository, times(2)).insertarSiNoExiste("CLIENTE");
    }
}
