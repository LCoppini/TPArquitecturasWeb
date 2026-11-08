package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.request.ActualizarUsuarioRequest;
import com.ticketya.usuarios.dto.request.CrearUsuarioRequest;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.exception.SolicitudInvalidaException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, rolRepository, new UsuarioMapper());
    }

    @Test
    void crearUsuarioPersisteDatosYRoles() {
        Rol cliente = new Rol("CLIENTE");
        cliente.setIdRol(1L);
        when(usuarioRepository.existsByDni(123L)).thenReturn(false);
        when(rolRepository.findAllByTipoIn(Set.of("CLIENTE"))).thenReturn(java.util.List.of(cliente));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setIdUsuario(7L);
            return usuario;
        });

        var response = service.crear(new CrearUsuarioRequest(123L, " Ana ", " Pérez ", " Tandil ", Set.of("CLIENTE")));

        assertEquals(7L, response.idUsuario());
        assertEquals("Ana", response.nombre());
        assertEquals(Set.of("CLIENTE"), response.roles().stream().map(rol -> rol.tipo()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(response.activo());
    }

    @Test
    void crearRechazaDniYaUsadoInclusoSinConsultarRoles() {
        when(usuarioRepository.existsByDni(123L)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> service.crear(new CrearUsuarioRequest(123L, "Ana", "Pérez", "Tandil", Set.of("CLIENTE"))));
    }

    @Test
    void bajaLogicaConservaLasAsociacionesDeRoles() {
        Rol cliente = new Rol("CLIENTE");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setDni(123L);
        usuario.setNombre("Ana");
        usuario.setRoles(Set.of(cliente));
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        service.eliminar(7L);

        assertFalse(usuario.isActivo());
        assertEquals(123L, usuario.getDni());
        assertEquals("Ana", usuario.getNombre());
        assertEquals(Set.of(cliente), usuario.getRoles());
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void asignacionRechazaRolesDesconocidosSinCambiarAsociaciones() {
        Rol cliente = new Rol("CLIENTE");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setRoles(Set.of(cliente));
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findAllByTipoIn(Set.of("ADMINISTRADOR"))).thenReturn(java.util.List.of());

        assertThrows(SolicitudInvalidaException.class,
                () -> service.reemplazarRoles(7L, Set.of("ADMINISTRADOR")));
        assertEquals(Set.of(cliente), usuario.getRoles());
    }

    @Test
    void asignacionRechazaUsuarioInactivo() {
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reemplazarRoles(7L, Set.of("CLIENTE")));
    }

    @Test
    void actualizarUsuarioInactivoDevuelveNoEncontrado() {
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(7L, new ActualizarUsuarioRequest(123L, "Ana", "Pérez", "Tandil")));
    }

    @Test
    void actualizarPermiteConservarElDniDelMismoUsuario() {
        Usuario usuario = usuarioActivo(7L, 123L);
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByDniAndIdUsuarioNot(123L, 7L)).thenReturn(false);
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var actualizado = service.actualizar(7L, new ActualizarUsuarioRequest(123L, "Ana", "Pérez", "Tandil"));

        assertEquals("Tandil", actualizado.ciudad());
    }

    @Test
    void actualizarRechazaDniDeOtroUsuario() {
        Usuario usuario = usuarioActivo(7L, 123L);
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByDniAndIdUsuarioNot(456L, 7L)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> service.actualizar(7L, new ActualizarUsuarioRequest(456L, "Ana", "Pérez", "Tandil")));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void reemplazarRolesAceptaListaVacia() {
        Usuario usuario = usuarioActivo(7L, 123L);
        Rol cliente = new Rol("CLIENTE");
        usuario.setRoles(Set.of(cliente));
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var actualizado = service.reemplazarRoles(7L, Set.of());

        assertTrue(actualizado.roles().isEmpty());
        assertTrue(usuario.getRoles().isEmpty());
    }

    @Test
    void reemplazarRolesPersisteMultiplesRolesDeFormaIdempotente() {
        Usuario usuario = usuarioActivo(7L, 123L);
        Rol cliente = new Rol("CLIENTE");
        cliente.setIdRol(1L);
        Rol organizador = new Rol("ORGANIZADOR");
        organizador.setIdRol(2L);
        Set<String> tipos = Set.of("CLIENTE", "ORGANIZADOR");
        when(usuarioRepository.findByIdUsuarioAndActivoTrue(7L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findAllByTipoIn(tipos)).thenReturn(java.util.List.of(cliente, organizador));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        service.reemplazarRoles(7L, tipos);
        var actualizado = service.reemplazarRoles(7L, tipos);

        assertEquals(2, actualizado.roles().size());
        assertEquals(tipos, actualizado.roles().stream().map(rol -> rol.tipo()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, usuario.getRoles().size());
    }

    @Test
    void listarDevuelveSoloLosUsuariosActivosDelRepositorio() {
        Usuario usuario = usuarioActivo(7L, 123L);
        when(usuarioRepository.findAllByActivoTrueOrderByIdUsuarioAsc()).thenReturn(java.util.List.of(usuario));

        var usuarios = service.listar();

        assertEquals(1, usuarios.size());
        assertEquals(7L, usuarios.getFirst().idUsuario());
    }

    private Usuario usuarioActivo(Long id, Long dni) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setDni(dni);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setCiudad("Tandil");
        usuario.setActivo(true);
        return usuario;
    }
}
