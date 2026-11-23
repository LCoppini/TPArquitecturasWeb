package com.ticketya.usuarios.config;

import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Usuarios de demostración, uno por rol, solo con el perfil {@code dev-seed}.
 * Todos comparten la contraseña {@code seed.dev.password}. Es idempotente: omite los correos que ya existen.
 * El orden fija los ids en una base vacía: ADMINISTRADOR 1, ORGANIZADOR 2, clientes 3 y 4, acceso 5.
 */
@Component
@Profile("dev-seed")
public class DatosDemoUsuariosSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoUsuariosSeeder.class);

    private record UsuarioDemo(long dni, String nombre, String apellido, String ciudad, String email, String rol) {
    }

    private static final List<UsuarioDemo> USUARIOS = List.of(
            new UsuarioDemo(20000001L, "Ana", "Admin", "Tandil", "admin@ticketya.dev", "ADMINISTRADOR"),
            new UsuarioDemo(20000002L, "Omar", "Organizador", "Buenos Aires", "organizador@ticketya.dev", "ORGANIZADOR"),
            new UsuarioDemo(20000003L, "Valentina", "Ruiz", "Tandil", "cliente@ticketya.dev", "CLIENTE"),
            new UsuarioDemo(20000004L, "Mateo", "Gómez", "Rosario", "cliente2@ticketya.dev", "CLIENTE"),
            new UsuarioDemo(20000005L, "Paula", "Acceso", "Buenos Aires", "acceso@ticketya.dev", "PERSONAL_ACCESO")
    );

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DatosDemoUsuariosSeeder(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${seed.dev.password:Ticketya123!}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creados = 0;
        for (UsuarioDemo demo : USUARIOS) {
            if (usuarioRepository.existsByEmail(demo.email())) {
                continue;
            }
            usuarioRepository.saveAndFlush(construir(demo));
            creados++;
        }
        log.info("Datos de demostración de usuarios: {} creados, {} ya existían.", creados, USUARIOS.size() - creados);
    }

    private Usuario construir(UsuarioDemo demo) {
        rolRepository.insertarSiNoExiste(demo.rol());
        Rol rol = rolRepository.findAllByTipoIn(Set.of(demo.rol())).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo cargar el rol " + demo.rol() + "."));
        Usuario usuario = new Usuario();
        usuario.setDni(demo.dni());
        usuario.setNombre(demo.nombre());
        usuario.setApellido(demo.apellido());
        usuario.setCiudad(demo.ciudad());
        usuario.setEmail(demo.email());
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRoles(new HashSet<>(Set.of(rol)));
        return usuario;
    }
}
