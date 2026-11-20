package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.LoginRequest;
import com.ticketya.usuarios.dto.response.LoginResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.CredencialesInvalidasException;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.impl.AutenticacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private AutenticacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AutenticacionServiceImpl(usuarioRepository, passwordEncoder, tokenService);
    }

    @Test
    void loginValidoEmiteTokenConRolesOrdenados() {
        Usuario usuario = usuarioConRoles(Set.of("ORGANIZADOR", "CLIENTE"));
        usuario.setPasswordHash("hash-bcrypt");
        when(usuarioRepository.findByEmailAndActivoTrue("ana@ticketya.local")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreto123", "hash-bcrypt")).thenReturn(true);
        when(tokenService.emitir(7L, Set.of("CLIENTE", "ORGANIZADOR"))).thenReturn("jwt-firmado");
        when(tokenService.expiracionEnSegundos()).thenReturn(900L);

        LoginResponse respuesta = service.login(new LoginRequest(" ANA@Ticketya.local ", "secreto123"));

        assertEquals("jwt-firmado", respuesta.token());
        assertEquals("Bearer", respuesta.tipo());
        assertEquals(900, respuesta.expiraEnSegundos());
    }

    @Test
    void passwordIncorrectoDevuelveErrorGenericoSinEmitirToken() {
        Usuario usuario = usuarioConRoles(Set.of("CLIENTE"));
        usuario.setPasswordHash("hash-bcrypt");
        when(usuarioRepository.findByEmailAndActivoTrue("ana@ticketya.local")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecto", "hash-bcrypt")).thenReturn(false);

        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("ana@ticketya.local", "incorrecto")));

        assertEquals(CredencialesInvalidasException.MENSAJE, error.getMessage());
        verify(tokenService, never()).emitir(any(), any());
    }

    @Test
    void emailInexistenteDevuelveElMismoErrorGenerico() {
        when(usuarioRepository.findByEmailAndActivoTrue("no@ticketya.local")).thenReturn(Optional.empty());

        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("no@ticketya.local", "secreto123")));

        assertEquals(CredencialesInvalidasException.MENSAJE, error.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void usuarioLegacySinHashNoPuedeIngresar() {
        when(usuarioRepository.findByEmailAndActivoTrue("ana@ticketya.local"))
                .thenReturn(Optional.of(usuarioConRoles(Set.of("CLIENTE"))));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("ana@ticketya.local", "secreto123")));
        verify(tokenService, never()).emitir(any(), any());
    }

    @Test
    void usuarioSinRolesNoPuedeIngresar() {
        Usuario usuario = usuarioConRoles(Set.of());
        usuario.setPasswordHash("hash-bcrypt");
        when(usuarioRepository.findByEmailAndActivoTrue("ana@ticketya.local")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreto123", "hash-bcrypt")).thenReturn(true);

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("ana@ticketya.local", "secreto123")));
        verify(tokenService, never()).emitir(any(), any());
    }

    private Usuario usuarioConRoles(Set<String> tipos) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setEmail("ana@ticketya.local");
        usuario.setRoles(tipos.stream().map(tipo -> {
            Rol rol = new Rol(tipo);
            rol.setIdRol((long) tipo.hashCode());
            return rol;
        }).collect(java.util.stream.Collectors.toSet()));
        return usuario;
    }
}
