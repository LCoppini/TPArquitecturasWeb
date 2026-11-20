package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.CrearUsuarioAdministrativoRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.SolicitudInvalidaException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.impl.AdminUsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUsuarioServiceImpl(usuarioRepository, rolRepository, passwordEncoder, new UsuarioMapper());
    }

    @Test
    void crearAdministrativoAsignaRolesSolicitadosYGuardaPasswordHasheado() {
        Rol organizador = new Rol("ORGANIZADOR");
        organizador.setIdRol(2L);
        when(usuarioRepository.existsByEmail("org@ticketya.local")).thenReturn(false);
        when(usuarioRepository.existsByDni(123L)).thenReturn(false);
        when(rolRepository.findAllByTipoIn(Set.of("ORGANIZADOR"))).thenReturn(List.of(organizador));
        when(passwordEncoder.encode("secreto123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setIdUsuario(9L);
            return usuario;
        });

        UsuarioResponse respuesta = service.crear(solicitud("ORGANIZADOR"));

        assertEquals(9L, respuesta.idUsuario());
        assertEquals(Set.of("ORGANIZADOR"), respuesta.roles().stream()
                .map(Rol -> Rol.tipo()).collect(Collectors.toSet()));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertEquals("org@ticketya.local", captor.getValue().getEmail());
        assertEquals("hash-bcrypt", captor.getValue().getPasswordHash());
        assertEquals(Set.of(organizador), captor.getValue().getRoles());
    }

    @Test
    void crearAdministrativoRechazaRolDesconocido() {
        when(usuarioRepository.existsByEmail("org@ticketya.local")).thenReturn(false);
        when(usuarioRepository.existsByDni(123L)).thenReturn(false);
        when(rolRepository.findAllByTipoIn(Set.of("INSTITUCION"))).thenReturn(List.of());

        assertThrows(SolicitudInvalidaException.class, () -> service.crear(solicitud("INSTITUCION")));
        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void crearAdministrativoRechazaEmailDuplicado() {
        when(usuarioRepository.existsByEmail("org@ticketya.local")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(solicitud("ORGANIZADOR")));
        verify(rolRepository, never()).findAllByTipoIn(any());
    }

    private CrearUsuarioAdministrativoRequest solicitud(String rol) {
        return new CrearUsuarioAdministrativoRequest(
                123L, " Ana ", " Pérez ", " Tandil ", " ORG@Ticketya.local ", "secreto123", Set.of(rol)
        );
    }
}
