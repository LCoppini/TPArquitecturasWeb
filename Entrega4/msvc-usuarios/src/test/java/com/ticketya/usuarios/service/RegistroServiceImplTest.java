package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.RegistroRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.impl.RegistroServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistroServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegistroServiceImpl(usuarioRepository, rolRepository, passwordEncoder, new UsuarioMapper());
    }

    @Test
    void registroFuerzaRolClienteYGuardaPasswordHasheado() {
        Rol cliente = new Rol("CLIENTE");
        cliente.setIdRol(1L);
        when(usuarioRepository.existsByEmail("ana@ticketya.local")).thenReturn(false);
        when(usuarioRepository.existsByDni(123L)).thenReturn(false);
        when(rolRepository.findAllByTipoIn(Set.of("CLIENTE"))).thenReturn(List.of(cliente));
        when(passwordEncoder.encode("secreto123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setIdUsuario(9L);
            return usuario;
        });

        UsuarioResponse respuesta = service.registrar(registro(" ANA@Ticketya.local "));

        assertEquals(9L, respuesta.idUsuario());
        assertEquals(Set.of("CLIENTE"), respuesta.roles().stream().map(rol -> rol.tipo()).collect(java.util.stream.Collectors.toSet()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertEquals("ana@ticketya.local", captor.getValue().getEmail());
        assertEquals("hash-bcrypt", captor.getValue().getPasswordHash());
        assertEquals(Set.of(cliente), captor.getValue().getRoles());
    }

    @Test
    void registroRechazaEmailDuplicadoSinConsultarRoles() {
        when(usuarioRepository.existsByEmail("ana@ticketya.local")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.registrar(registro("ana@ticketya.local")));
        verify(rolRepository, never()).findAllByTipoIn(any());
    }

    @Test
    void registroRechazaDniDuplicado() {
        when(usuarioRepository.existsByEmail("ana@ticketya.local")).thenReturn(false);
        when(usuarioRepository.existsByDni(123L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.registrar(registro("ana@ticketya.local")));
        verify(passwordEncoder, never()).encode(any());
    }

    private RegistroRequest registro(String email) {
        return new RegistroRequest(123L, " Ana ", " Pérez ", " Tandil ", email, "secreto123");
    }
}
