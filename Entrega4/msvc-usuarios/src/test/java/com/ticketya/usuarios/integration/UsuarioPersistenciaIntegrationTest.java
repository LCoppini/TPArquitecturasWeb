package com.ticketya.usuarios.integration;

import com.ticketya.usuarios.config.RolSeeder;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ticketya_usuarios",
        "spring.datasource.username=ticketya",
        "spring.datasource.password=ticketya",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RolSeeder.class)
@EnabledIfSystemProperty(named = "usuarios.integration", matches = "true")
class UsuarioPersistenciaIntegrationTest {

    private static final ApplicationArguments ARGUMENTS = new DefaultApplicationArguments();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private RolSeeder rolSeeder;

    @Test
    void inicializaCatalogoDeRolesSinDuplicados() {
        rolSeeder.run(ARGUMENTS);
        rolSeeder.run(ARGUMENTS);

        assertEquals(Set.of("CLIENTE", "ORGANIZADOR", "ADMINISTRADOR", "PERSONAL_ACCESO"),
                rolRepository.findAllByOrderByTipoAsc().stream()
                        .map(Rol::getTipo)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(4, rolRepository.count());
    }

    @Test
    void bajaLogicaConservaUsuarioYAsociacionesDeRolesEnPostgres() {
        rolSeeder.run(ARGUMENTS);
        var cliente = rolRepository.findAllByTipoIn(Set.of("CLIENTE")).getFirst();
        Usuario usuario = usuario("Ana", 760000001L);
        usuario.setRoles(new HashSet<>(Set.of(cliente)));
        usuario = usuarioRepository.saveAndFlush(usuario);
        Long idUsuario = usuario.getIdUsuario();

        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);

        assertTrue(usuarioRepository.findByIdUsuarioAndActivoTrue(idUsuario).isEmpty());
        assertTrue(usuarioRepository.findAllByActivoTrueOrderByIdUsuarioAsc().stream()
                .noneMatch(usuarioActivo -> usuarioActivo.getIdUsuario().equals(idUsuario)));
        Usuario persistido = usuarioRepository.findById(idUsuario).orElseThrow();
        assertFalse(persistido.isActivo());
        assertEquals(760000001L, persistido.getDni());
        assertEquals("Ana", persistido.getNombre());
        assertTrue(usuarioRepository.existsByDni(760000001L));
        assertEquals(Set.of("CLIENTE"), persistido.getRoles().stream()
                .map(Rol::getTipo)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void restriccionUnicaDeDniIncluyeUsuariosInactivos() {
        Usuario inactivo = usuario("Inactivo", 760000002L);
        inactivo.setActivo(false);
        inactivo = usuarioRepository.saveAndFlush(inactivo);
        assertTrue(usuarioRepository.existsByDni(760000002L));
        assertTrue(usuarioRepository.existsByDniAndIdUsuarioNot(760000002L, -1L));
        assertFalse(usuarioRepository.existsByDniAndIdUsuarioNot(760000002L, inactivo.getIdUsuario()));

        Usuario duplicado = usuario("Duplicado", 760000002L);
        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(duplicado));
    }

    private Usuario usuario(String nombre, Long dni) {
        Usuario usuario = new Usuario();
        usuario.setDni(dni);
        usuario.setNombre(nombre);
        usuario.setApellido("Prueba");
        usuario.setCiudad("Tandil");
        return usuario;
    }
}
